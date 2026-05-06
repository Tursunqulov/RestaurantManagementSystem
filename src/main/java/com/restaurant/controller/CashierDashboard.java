package com.restaurant.controller;

import com.restaurant.dao.BillDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.PaymentDAO;
import com.restaurant.model.Bill;
import com.restaurant.model.CashPayment;
import com.restaurant.model.CheckPayment;
import com.restaurant.model.CreditCardPayment;
import com.restaurant.model.User;
import com.restaurant.model.enums.OrderStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
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
import java.util.List;

public class CashierDashboard {

    private final User       user;
    private final BillDAO    billDAO    = new BillDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderDAO   orderDAO   = new OrderDAO();

    private final ObservableList<Bill> unpaidBills = FXCollections.observableArrayList();
    private final Stage stage = new Stage();

    private CashierDashboard(User user) { this.user = user; }

    public static void open(User user) { new CashierDashboard(user).show(); }

    private void show() {
        stage.setTitle("Cashier Dashboard — " + user.getUsername());
        stage.setMinWidth(900);
        stage.setMinHeight(640);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #1a1a2e;");
        tabs.getTabs().add(buildBillsTab());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setTop(buildTopBar());
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 960, 700));
        stage.show();
        loadUnpaidBills();
    }

    // ── Top bar ──────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #16213e;");
        Label userLbl = new Label("Cashier: " + user.getUsername());
        userLbl.setStyle("-fx-text-fill: #a8a8b3; -fx-font-size: 13;");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutBtn = btn("Logout");
        logoutBtn.setOnAction(e -> { stage.close(); com.restaurant.Main.showLogin(); });
        bar.getChildren().addAll(userLbl, spacer, logoutBtn);
        return bar;
    }

    // ── Bills tab ────────────────────────────────────────────────────────

    private Tab buildBillsTab() {
        Tab tab = new Tab("Process Payments");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label heading = lbl("Unpaid Bills", 18, "#e94560", true);

        TableView<Bill> billView = new TableView<>(unpaidBills);
        billView.getColumns().addAll(
                col("Bill ID",  "billId",      70),
                col("Order ID", "orderId",     80),
                col("Table",    "tableNumber", 90),
                col("Subtotal", "amount",      110),
                col("Tax",      "tax",         90),
                col("Total",    "total",       110));
        billView.setPrefHeight(260);

        Label summaryLbl = lbl("Select a bill above to process payment.", 13, "#a8a8b3", false);

        billView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            summaryLbl.setText(String.format(
                    "Bill #%d | Order #%d | Table: %s | Total: $%.2f",
                    n.getBillId(), n.getOrderId(),
                    n.getTableNumber() != null ? n.getTableNumber() : "?",
                    n.getTotal()));
        });

        Button refreshBtn = btn("Refresh");
        refreshBtn.setOnAction(e -> loadUnpaidBills());
        HBox refreshRow = new HBox(10, refreshBtn, summaryLbl);
        refreshRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        Label payHeading = lbl("Payment Method", 15, "#e0e0e0", true);

        TitledPane cashPane  = new TitledPane("Cash",        buildCashForm(billView));
        TitledPane ccPane    = new TitledPane("Credit Card", buildCCForm(billView));
        TitledPane checkPane = new TitledPane("Check",       buildCheckForm(billView));
        Accordion accordion  = new Accordion(cashPane, ccPane, checkPane);
        accordion.setExpandedPane(cashPane);

        root.getChildren().addAll(heading, refreshRow, billView, sep, payHeading, accordion);
        tab.setContent(new ScrollPane(root));
        return tab;
    }

    // ── Payment forms ────────────────────────────────────────────────────

    private VBox buildCashForm(TableView<Bill> billView) {
        VBox box = new VBox(10); box.setPadding(new Insets(10));
        TextField tenderedField = new TextField(); tenderedField.setPromptText("Cash tendered");
        Label result = new Label(); result.setStyle("-fx-text-fill: #4ade80;");
        Button payBtn = btn("Pay Cash");
        payBtn.setOnAction(e -> {
            Bill sel = billView.getSelectionModel().getSelectedItem();
            if (sel == null) { result.setText("Select a bill first."); return; }
            double tendered;
            try { tendered = Double.parseDouble(tenderedField.getText().trim()); }
            catch (NumberFormatException ex) { result.setText("Invalid amount."); return; }
            int    billId  = sel.getBillId();
            int    orderId = sel.getOrderId();
            double total   = sel.getTotal();
            if (tendered < total) {
                result.setText(String.format("Insufficient — need $%.2f", total));
                return;
            }
            double finalTendered = tendered;
            result.setText("Processing…");
            new Thread(() -> {
                try {
                    CashPayment pay = new CashPayment(0, billId, total, finalTendered);
                    paymentDAO.insert(pay);
                    billDAO.markPaid(billId);
                    orderDAO.updateStatus(orderId, OrderStatus.COMPLETE);
                    Platform.runLater(() -> {
                        result.setText(String.format("Paid. Change: $%.2f", pay.getChange()));
                        unpaidBills.remove(sel);
                        tenderedField.clear();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> result.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });
        box.getChildren().addAll(tenderedField, payBtn, result);
        return box;
    }

    private VBox buildCCForm(TableView<Bill> billView) {
        VBox box = new VBox(10); box.setPadding(new Insets(10));
        TextField nameField = new TextField(); nameField.setPromptText("Name on card");
        TextField last4     = new TextField(); last4.setPromptText("Last 4 digits");
        Label result = new Label(); result.setStyle("-fx-text-fill: #4ade80;");
        Button payBtn = btn("Charge Card");
        payBtn.setOnAction(e -> {
            Bill sel = billView.getSelectionModel().getSelectedItem();
            if (sel == null) { result.setText("Select a bill first."); return; }
            int    billId  = sel.getBillId();
            int    orderId = sel.getOrderId();
            double total   = sel.getTotal();
            String name    = nameField.getText().trim();
            String last    = last4.getText().trim();
            result.setText("Processing…");
            new Thread(() -> {
                try {
                    CreditCardPayment pay = new CreditCardPayment(0, billId, total, name, last);
                    paymentDAO.insert(pay);
                    billDAO.markPaid(billId);
                    orderDAO.updateStatus(orderId, OrderStatus.COMPLETE);
                    Platform.runLater(() -> {
                        result.setText("Card charged. " + pay.getPaymentDetail());
                        unpaidBills.remove(sel);
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> result.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });
        box.getChildren().addAll(nameField, last4, payBtn, result);
        return box;
    }

    private VBox buildCheckForm(TableView<Bill> billView) {
        VBox box = new VBox(10); box.setPadding(new Insets(10));
        TextField bankField = new TextField(); bankField.setPromptText("Bank name");
        TextField checkNum  = new TextField(); checkNum.setPromptText("Check number");
        Label result = new Label(); result.setStyle("-fx-text-fill: #4ade80;");
        Button payBtn = btn("Accept Check");
        payBtn.setOnAction(e -> {
            Bill sel = billView.getSelectionModel().getSelectedItem();
            if (sel == null) { result.setText("Select a bill first."); return; }
            int    billId  = sel.getBillId();
            int    orderId = sel.getOrderId();
            double total   = sel.getTotal();
            String bank    = bankField.getText().trim();
            String check   = checkNum.getText().trim();
            result.setText("Processing…");
            new Thread(() -> {
                try {
                    CheckPayment pay = new CheckPayment(0, billId, total, bank, check);
                    paymentDAO.insert(pay);
                    billDAO.markPaid(billId);
                    orderDAO.updateStatus(orderId, OrderStatus.COMPLETE);
                    Platform.runLater(() -> {
                        result.setText("Check accepted. " + pay.getPaymentDetail());
                        unpaidBills.remove(sel);
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> result.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });
        box.getChildren().addAll(bankField, checkNum, payBtn, result);
        return box;
    }

    // ── Async loaders ─────────────────────────────────────────────────────

    private void loadUnpaidBills() {
        int branchId = user.getBranchId() != null ? user.getBranchId() : 1;
        new Thread(() -> {
            try {
                List<Bill> list = billDAO.findUnpaidByBranch(branchId);
                Platform.runLater(() -> unpaidBills.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> System.err.println("loadUnpaidBills: " + e.getMessage()));
            }
        }).start();
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
