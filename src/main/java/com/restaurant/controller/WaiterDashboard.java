package com.restaurant.controller;

import com.restaurant.dao.BillDAO;
import com.restaurant.dao.MenuDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.PaymentDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.model.Bill;
import com.restaurant.model.CashPayment;
import com.restaurant.model.CheckPayment;
import com.restaurant.model.CreditCardPayment;
import com.restaurant.model.Meal;
import com.restaurant.model.MealItem;
import com.restaurant.model.MenuItem;
import com.restaurant.model.Order;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableSeat;
import com.restaurant.model.User;
import com.restaurant.model.enums.OrderStatus;
import com.restaurant.model.enums.TableStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WaiterDashboard {

    private final User       user;
    private final TableDAO   tableDAO   = new TableDAO();
    private final OrderDAO   orderDAO   = new OrderDAO();
    private final MenuDAO    menuDAO    = new MenuDAO();
    private final BillDAO    billDAO    = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private final ObservableList<RestaurantTable> tables     = FXCollections.observableArrayList();
    private final ObservableList<MenuItem>        menuItems  = FXCollections.observableArrayList();
    private final ObservableList<MealItem>        orderItems = FXCollections.observableArrayList();
    private final ObservableList<String>          orderLog   = FXCollections.observableArrayList();

    // Always read/written on the FX thread
    private Order  activeOrder;
    private Meal   activeMeal;
    private double lastSubtotal = 0;
    private double lastTax      = 0;

    private final Stage stage = new Stage();

    // Shared bill summary labels (updated by Calculate Bill button)
    private final Label subtotalLbl = lbl("Subtotal: $0.00",     14, "#4ade80", false);
    private final Label taxLbl      = lbl("Tax (10%): $0.00",    13, "#a8a8b3", false);
    private final Label grandLbl    = lbl("Grand Total: $0.00",  16, "#e94560", true);

    private WaiterDashboard(User user) { this.user = user; }

    public static void open(User user) { new WaiterDashboard(user).show(); }

    private void show() {
        stage.setTitle("Waiter Dashboard — " + user.getUsername());
        stage.setMinWidth(1060);
        stage.setMinHeight(700);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #1a1a2e;");
        tabs.getTabs().addAll(buildTableTab(), buildOrderTab(), buildUpdateTab(), buildBillTab());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setTop(buildTopBar());
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 1100, 760));
        stage.show();
        loadTables();
        loadMenuItems();
    }

    // ── Top bar ──────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #16213e;");
        Label userLbl = new Label("Waiter: " + user.getUsername());
        userLbl.setStyle("-fx-text-fill: #a8a8b3; -fx-font-size: 13;");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutBtn = btn("Logout");
        logoutBtn.setOnAction(e -> { stage.close(); com.restaurant.Main.showLogin(); });
        bar.getChildren().addAll(userLbl, spacer, logoutBtn);
        return bar;
    }

    // ── Tab 1: Tables ────────────────────────────────────────────────────

    private Tab buildTableTab() {
        Tab tab = new Tab("Tables");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label heading = lbl("Table Layout", 18, "#e94560", true);

        TableView<RestaurantTable> tv = new TableView<>(tables);
        tv.getColumns().addAll(
                col("ID",       "tableId",            60),
                col("Table #",  "tableNumber",        100),
                col("Capacity", "maxCapacity",         90),
                col("Location", "locationIdentifier", 200),
                col("Status",   "status",             120));
        tv.setPrefHeight(340);

        Label status = new Label(); status.setStyle("-fx-text-fill: #4ade80;");
        Button startBtn   = btn("Start / Resume Order");
        Button refreshBtn = btn("Refresh");

        startBtn.setOnAction(e -> {
            RestaurantTable sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { status.setText("Select a table first."); return; }
            int    tableId  = sel.getTableId();
            String tableNum = sel.getTableNumber();
            status.setText("Working…");
            new Thread(() -> {
                try {
                    Order existing = orderDAO.findActiveOrderForTable(tableId);
                    if (existing != null) {
                        Platform.runLater(() -> {
                            activeOrder = existing;
                            status.setText("Resumed order #" + existing.getOrderId() + " – Table " + tableNum);
                            refreshOrderLogAsync();
                        });
                    } else {
                        Order newOrder = new Order(0, tableId, user.getUserId());
                        orderDAO.insert(newOrder);
                        tableDAO.updateStatus(tableId, TableStatus.OCCUPIED);
                        Platform.runLater(() -> {
                            activeOrder = newOrder;
                            status.setText("Started order #" + newOrder.getOrderId() + " – Table " + tableNum);
                            loadTables();
                        });
                    }
                } catch (SQLException ex) {
                    Platform.runLater(() -> status.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        refreshBtn.setOnAction(e -> loadTables());

        HBox bar = new HBox(10, startBtn, refreshBtn, status);
        bar.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(heading, tv, bar);
        tab.setContent(root);
        return tab;
    }

    // ── Tab 2: Add Items ─────────────────────────────────────────────────

    private Tab buildOrderTab() {
        Tab tab = new Tab("Add Items");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label heading = lbl("Add Meal Items to Seat", 18, "#e94560", true);

        HBox seatRow = new HBox(10); seatRow.setAlignment(Pos.CENTER_LEFT);
        Spinner<Integer> seatSpinner = new Spinner<>(1, 30, 1); seatSpinner.setPrefWidth(80);
        Button selectSeatBtn = btn("Select Seat");
        Label seatStatus = new Label(); seatStatus.setStyle("-fx-text-fill: #a8a8b3;");
        seatRow.getChildren().addAll(lbl("Seat #:", 13, "#e0e0e0", false), seatSpinner,
                selectSeatBtn, seatStatus);

        Label menuHeading = lbl("Menu Items", 14, "#e0e0e0", true);
        TableView<MenuItem> menuView = new TableView<>(menuItems);
        menuView.getColumns().addAll(
                col("ID",    "itemId", 60),
                col("Name",  "title",  220),
                col("Price", "price",  100));
        menuView.setPrefHeight(210);

        HBox addRow = new HBox(10); addRow.setAlignment(Pos.CENTER_LEFT);
        Spinner<Integer> qtySpinner = new Spinner<>(1, 50, 1); qtySpinner.setPrefWidth(70);
        Button addItemBtn = btn("Add to Meal");
        Label addStatus = new Label(); addStatus.setStyle("-fx-text-fill: #4ade80;");
        addRow.getChildren().addAll(lbl("Qty:", 13, "#e0e0e0", false), qtySpinner,
                addItemBtn, addStatus);

        Label logHeading = lbl("Current Order", 14, "#e0e0e0", true);
        ListView<String> logView = new ListView<>(orderLog);
        logView.setPrefHeight(130);

        selectSeatBtn.setOnAction(e -> {
            if (activeOrder == null) { seatStatus.setText("Start an order first (Tables tab)."); return; }
            int seatNum = seatSpinner.getValue();
            int tableId = activeOrder.getTableId();
            int orderId = activeOrder.getOrderId();
            seatStatus.setText("Finding seat…");
            new Thread(() -> {
                try {
                    List<TableSeat> seats = tableDAO.findSeatsByTable(tableId);
                    TableSeat seat = seats.stream()
                            .filter(s -> s.getSeatNumber() == seatNum)
                            .findFirst().orElse(null);
                    if (seat == null) {
                        Platform.runLater(() -> seatStatus.setText("Seat " + seatNum + " not found."));
                        return;
                    }
                    Meal meal = orderDAO.getOrCreateMeal(orderId, seat.getId());
                    Platform.runLater(() -> {
                        activeMeal = meal;
                        seatStatus.setText("Seat " + seatNum + " active (meal #" + meal.getMealId() + ")");
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> seatStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        addItemBtn.setOnAction(e -> {
            if (activeMeal == null) { addStatus.setText("Select a seat first."); return; }
            MenuItem sel = menuView.getSelectionModel().getSelectedItem();
            if (sel == null) { addStatus.setText("Select a menu item."); return; }
            int mealId = activeMeal.getMealId();
            int itemId = sel.getItemId();
            int qty    = qtySpinner.getValue();
            String name = sel.getTitle();
            new Thread(() -> {
                try {
                    MealItem mi = new MealItem(0, mealId, itemId, qty);
                    orderDAO.insertMealItem(mi);
                    Platform.runLater(() -> {
                        addStatus.setText("Added: " + qty + "x " + name);
                        refreshOrderLogAsync();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> addStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(heading, seatRow, menuHeading, menuView, addRow, logHeading, logView);
        tab.setContent(root);
        return tab;
    }

    // ── Tab 3: Update Order ──────────────────────────────────────────────

    private Tab buildUpdateTab() {
        Tab tab = new Tab("Update Order");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label heading = lbl("Modify or Remove Items", 18, "#e94560", true);

        TableView<MealItem> itemsView = new TableView<>(orderItems);
        itemsView.getColumns().addAll(
                col("Item ID",    "mealItemId",    70),
                col("Name",       "menuItemTitle", 220),
                col("Price",      "menuItemPrice",  90),
                col("Qty",        "quantity",       60),
                col("Line Total", "lineTotal",      100));
        itemsView.setPrefHeight(280);

        Label editStatus = new Label(); editStatus.setStyle("-fx-text-fill: #4ade80;");
        Button refreshBtn = btn("Refresh Items");
        refreshBtn.setOnAction(e -> {
            if (activeOrder == null) { editStatus.setText("No active order."); return; }
            loadCurrentOrderItemsAsync(editStatus);
        });

        HBox editRow = new HBox(10); editRow.setAlignment(Pos.CENTER_LEFT);
        Spinner<Integer> qtySpinner = new Spinner<>(1, 50, 1); qtySpinner.setPrefWidth(70);
        Button updateQtyBtn = btn("Update Qty");
        Button removeBtn    = btn("Remove Item");
        editRow.getChildren().addAll(lbl("New Qty:", 13, "#e0e0e0", false), qtySpinner,
                updateQtyBtn, removeBtn, editStatus);

        updateQtyBtn.setOnAction(e -> {
            MealItem sel = itemsView.getSelectionModel().getSelectedItem();
            if (sel == null) { editStatus.setText("Select an item."); return; }
            int mealItemId = sel.getMealItemId();
            int newQty     = qtySpinner.getValue();
            new Thread(() -> {
                try {
                    orderDAO.updateMealItemQuantity(mealItemId, newQty);
                    Platform.runLater(() -> {
                        sel.setQuantity(newQty);
                        itemsView.refresh();
                        editStatus.setText("Qty updated to " + newQty + ".");
                        refreshOrderLogAsync();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> editStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        removeBtn.setOnAction(e -> {
            MealItem sel = itemsView.getSelectionModel().getSelectedItem();
            if (sel == null) { editStatus.setText("Select an item."); return; }
            int mealItemId = sel.getMealItemId();
            new Thread(() -> {
                try {
                    orderDAO.deleteMealItem(mealItemId);
                    Platform.runLater(() -> {
                        orderItems.remove(sel);
                        editStatus.setText("Item removed.");
                        refreshOrderLogAsync();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> editStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(heading, refreshBtn, itemsView, editRow);
        tab.setContent(root);
        return tab;
    }

    // ── Tab 4: Bill & Payment ────────────────────────────────────────────

    private Tab buildBillTab() {
        Tab tab = new Tab("Bill & Payment");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label heading = lbl("Generate Bill & Process Payment", 18, "#e94560", true);

        Button calcBtn = btn("Calculate Bill");
        calcBtn.setOnAction(e -> {
            if (activeOrder == null) { grandLbl.setText("No active order."); return; }
            int orderId = activeOrder.getOrderId();
            new Thread(() -> {
                try {
                    Order refreshed = orderDAO.findById(orderId);
                    if (refreshed == null) return;
                    double sub = refreshed.calculateTotal();
                    double tax = sub * 0.10;
                    Platform.runLater(() -> {
                        lastSubtotal = sub;
                        lastTax      = tax;
                        subtotalLbl.setText(String.format("Subtotal: $%.2f", sub));
                        taxLbl.setText(String.format("Tax (10%%): $%.2f", tax));
                        grandLbl.setText(String.format("Grand Total: $%.2f", sub + tax));
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> grandLbl.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        HBox tipRow = new HBox(10); tipRow.setAlignment(Pos.CENTER_LEFT);
        TextField tipField = new TextField("0.00"); tipField.setPrefWidth(90);
        tipField.textProperty().addListener((obs, old, nv) -> {
            try {
                double tip = nv.trim().isEmpty() ? 0.0 : Double.parseDouble(nv.trim());
                if (tip < 0) return;
                grandLbl.setText(String.format("Grand Total: $%.2f", lastSubtotal + lastTax + tip));
            } catch (NumberFormatException ignored) {}
        });
        tipRow.getChildren().addAll(lbl("Tip ($):", 13, "#e0e0e0", false), tipField);

        Separator sep = new Separator();
        Label payHeading = lbl("Payment Method", 15, "#e0e0e0", true);

        TitledPane cashPane  = new TitledPane("Cash",        buildCashForm(tipField));
        TitledPane ccPane    = new TitledPane("Credit Card", buildCCForm(tipField));
        TitledPane checkPane = new TitledPane("Check",       buildCheckForm(tipField));
        Accordion accordion  = new Accordion(cashPane, ccPane, checkPane);
        accordion.setExpandedPane(cashPane);

        root.getChildren().addAll(heading, calcBtn, subtotalLbl, taxLbl, tipRow, grandLbl,
                sep, payHeading, accordion);
        tab.setContent(new ScrollPane(root));
        return tab;
    }

    // ── Payment forms ────────────────────────────────────────────────────

    private VBox buildCashForm(TextField tipField) {
        VBox box = new VBox(10); box.setPadding(new Insets(10));
        TextField tenderedField = new TextField(); tenderedField.setPromptText("Cash tendered");
        Label result = new Label(); result.setStyle("-fx-text-fill: #4ade80;");
        Button payBtn = btn("Pay Cash");
        payBtn.setOnAction(e -> {
            if (activeOrder == null) { result.setText("No active order."); return; }
            double tendered, tip;
            try {
                tendered = Double.parseDouble(tenderedField.getText().trim());
                tip      = parseTip(tipField);
            } catch (NumberFormatException ex) { result.setText("Invalid amount."); return; }
            int    orderId  = activeOrder.getOrderId();
            int    tableId  = activeOrder.getTableId();
            double finalTip = tip;
            double finalTendered = tendered;
            result.setText("Processing…");
            new Thread(() -> {
                try {
                    Bill bill = getOrCreateBill(orderId, finalTip);
                    if (finalTendered < bill.getTotal()) {
                        double needed = bill.getTotal();
                        Platform.runLater(() -> result.setText(
                                String.format("Insufficient — total is $%.2f", needed)));
                        return;
                    }
                    CashPayment pay = new CashPayment(0, bill.getBillId(), bill.getTotal(), finalTendered);
                    paymentDAO.insert(pay);
                    billDAO.markPaid(bill.getBillId());
                    closeOrder(orderId, tableId);
                    Platform.runLater(() ->
                            result.setText(String.format("Paid. Change: $%.2f", pay.getChange())));
                } catch (SQLException ex) {
                    Platform.runLater(() -> result.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });
        box.getChildren().addAll(tenderedField, payBtn, result);
        return box;
    }

    private VBox buildCCForm(TextField tipField) {
        VBox box = new VBox(10); box.setPadding(new Insets(10));
        TextField nameField = new TextField(); nameField.setPromptText("Name on card");
        TextField last4     = new TextField(); last4.setPromptText("Last 4 digits");
        Label result = new Label(); result.setStyle("-fx-text-fill: #4ade80;");
        Button payBtn = btn("Charge Card");
        payBtn.setOnAction(e -> {
            if (activeOrder == null) { result.setText("No active order."); return; }
            double tip;
            try { tip = parseTip(tipField); } catch (NumberFormatException ex) { result.setText("Invalid tip."); return; }
            int    orderId = activeOrder.getOrderId();
            int    tableId = activeOrder.getTableId();
            String name    = nameField.getText().trim();
            String last    = last4.getText().trim();
            double finalTip = tip;
            result.setText("Processing…");
            new Thread(() -> {
                try {
                    Bill bill = getOrCreateBill(orderId, finalTip);
                    CreditCardPayment pay = new CreditCardPayment(0, bill.getBillId(), bill.getTotal(), name, last);
                    paymentDAO.insert(pay);
                    billDAO.markPaid(bill.getBillId());
                    closeOrder(orderId, tableId);
                    Platform.runLater(() -> result.setText("Card charged. " + pay.getPaymentDetail()));
                } catch (SQLException ex) {
                    Platform.runLater(() -> result.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });
        box.getChildren().addAll(nameField, last4, payBtn, result);
        return box;
    }

    private VBox buildCheckForm(TextField tipField) {
        VBox box = new VBox(10); box.setPadding(new Insets(10));
        TextField bankField = new TextField(); bankField.setPromptText("Bank name");
        TextField checkNum  = new TextField(); checkNum.setPromptText("Check number");
        Label result = new Label(); result.setStyle("-fx-text-fill: #4ade80;");
        Button payBtn = btn("Accept Check");
        payBtn.setOnAction(e -> {
            if (activeOrder == null) { result.setText("No active order."); return; }
            double tip;
            try { tip = parseTip(tipField); } catch (NumberFormatException ex) { result.setText("Invalid tip."); return; }
            int    orderId = activeOrder.getOrderId();
            int    tableId = activeOrder.getTableId();
            String bank    = bankField.getText().trim();
            String check   = checkNum.getText().trim();
            double finalTip = tip;
            result.setText("Processing…");
            new Thread(() -> {
                try {
                    Bill bill = getOrCreateBill(orderId, finalTip);
                    CheckPayment pay = new CheckPayment(0, bill.getBillId(), bill.getTotal(), bank, check);
                    paymentDAO.insert(pay);
                    billDAO.markPaid(bill.getBillId());
                    closeOrder(orderId, tableId);
                    Platform.runLater(() -> result.setText("Check accepted. " + pay.getPaymentDetail()));
                } catch (SQLException ex) {
                    Platform.runLater(() -> result.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });
        box.getChildren().addAll(bankField, checkNum, payBtn, result);
        return box;
    }

    // ── Async helpers ─────────────────────────────────────────────────────

    private void loadTables() {
        int branchId = user.getBranchId() != null ? user.getBranchId() : 1;
        new Thread(() -> {
            try {
                List<RestaurantTable> list = tableDAO.findByBranch(branchId);
                Platform.runLater(() -> tables.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> System.err.println("loadTables: " + e.getMessage()));
            }
        }).start();
    }

    private void loadMenuItems() {
        int branchId = user.getBranchId() != null ? user.getBranchId() : 1;
        new Thread(() -> {
            try {
                List<MenuItem> list = menuDAO.findAllItemsByBranch(branchId);
                Platform.runLater(() -> menuItems.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> System.err.println("loadMenuItems: " + e.getMessage()));
            }
        }).start();
    }

    private void refreshOrderLogAsync() {
        if (activeOrder == null) return;
        int orderId = activeOrder.getOrderId();
        new Thread(() -> {
            try {
                Order o = orderDAO.findById(orderId);
                if (o == null) return;
                List<String> lines = new ArrayList<>();
                lines.add("=== Order #" + o.getOrderId() + " ===");
                for (Meal meal : o.getMeals()) {
                    lines.add("  Seat " + meal.getSeatId() + ":");
                    for (MealItem item : meal.getItems()) lines.add("    " + item);
                }
                lines.add(String.format("  — Subtotal: $%.2f", o.calculateTotal()));
                Platform.runLater(() -> orderLog.setAll(lines));
            } catch (SQLException e) {
                Platform.runLater(() -> System.err.println("refreshLog: " + e.getMessage()));
            }
        }).start();
    }

    private void loadCurrentOrderItemsAsync(Label statusLbl) {
        int orderId = activeOrder.getOrderId();
        new Thread(() -> {
            try {
                Order o = orderDAO.findById(orderId);
                List<MealItem> all = new ArrayList<>();
                if (o != null) for (Meal m : o.getMeals()) all.addAll(m.getItems());
                Platform.runLater(() -> {
                    orderItems.setAll(all);
                    statusLbl.setText("Loaded " + all.size() + " item(s).");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> statusLbl.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    /** Called from a background thread. */
    private Bill getOrCreateBill(int orderId, double tip) throws SQLException {
        Bill existing = billDAO.findByOrderId(orderId);
        if (existing != null) {
            if (tip != existing.getTip()) billDAO.updateTip(existing.getBillId(), tip);
            existing.setTip(tip);
            return existing;
        }
        Order o   = orderDAO.findById(orderId);
        double sub = o != null ? o.calculateTotal() : 0;
        Bill bill  = new Bill(0, orderId, sub, sub * 0.10, tip);
        billDAO.insert(bill);
        return bill;
    }

    /** Called from a background thread. Clears activeOrder on the FX thread. */
    private void closeOrder(int orderId, int tableId) throws SQLException {
        orderDAO.updateStatus(orderId, OrderStatus.COMPLETE);
        tableDAO.updateStatus(tableId, TableStatus.FREE);
        Platform.runLater(() -> {
            activeOrder = null;
            activeMeal  = null;
            orderLog.clear();
            orderItems.clear();
            subtotalLbl.setText("Subtotal: $0.00");
            taxLbl.setText("Tax (10%): $0.00");
            grandLbl.setText("Grand Total: $0.00");
            loadTables();
        });
    }

    private double parseTip(TextField f) {
        String t = f.getText().trim();
        if (t.isEmpty()) return 0.0;
        double v = Double.parseDouble(t);
        if (v < 0) throw new NumberFormatException("Tip cannot be negative");
        return v;
    }

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String title, String prop, double w) {
        TableColumn<S, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private Label lbl(String text, int size, String color, boolean bold) {
        Label l = new Label(text);
        l.setFont(bold ? Font.font("System", FontWeight.BOLD, size) : Font.font("System", size));
        l.setStyle("-fx-text-fill: " + color + ";");
        return l;
    }

    private Button btn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand;");
        return b;
    }
}
