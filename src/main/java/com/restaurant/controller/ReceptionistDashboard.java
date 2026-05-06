package com.restaurant.controller;

import com.restaurant.dao.CustomerDAO;
import com.restaurant.dao.ReservationDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.model.Customer;
import com.restaurant.model.Reservation;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.User;
import com.restaurant.model.enums.ReservationStatus;
import com.restaurant.model.enums.TableStatus;
import com.restaurant.service.ReservationNotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ReceptionistDashboard {

    private static final String DARK_BG = "-fx-background-color: #1a1a2e;";

    private final User user;
    private final ReservationNotificationService notificationService;
    private final TableDAO       tableDAO       = new TableDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final CustomerDAO    customerDAO    = new CustomerDAO();

    private final ObservableList<RestaurantTable> availableTables = FXCollections.observableArrayList();
    private final ObservableList<Reservation>     reservations    = FXCollections.observableArrayList();
    private final ObservableList<Customer>        customers       = FXCollections.observableArrayList();
    private final ListView<String>                notifList       = new ListView<>();

    private final Stage stage = new Stage();

    private ReceptionistDashboard(User user, ReservationNotificationService svc) {
        this.user = user;
        this.notificationService = svc;
    }

    public static void open(User user, ReservationNotificationService svc) {
        new ReceptionistDashboard(user, svc).show();
    }

    private void show() {
        stage.setTitle("Receptionist Dashboard — " + user.getUsername());
        stage.setMinWidth(1000);
        stage.setMinHeight(660);

        notificationService.addListener(notif ->
                Platform.runLater(() -> notifList.getItems().add(0, "[ALERT] " + notif.getContent())));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle(DARK_BG);
        tabs.getTabs().addAll(buildSearchTab(), buildReservationTab(), buildCustomerTab(), buildNotifTab());

        BorderPane root = new BorderPane();
        root.setStyle(DARK_BG);
        root.setTop(buildTopBar());
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 1040, 720));
        stage.show();
        loadReservations();
        loadCustomers();
    }

    // ── Top bar ──────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #16213e;");
        Label userLbl = new Label("Receptionist: " + user.getUsername());
        userLbl.setStyle("-fx-text-fill: #a8a8b3; -fx-font-size: 13;");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutBtn = btn("Logout");
        logoutBtn.setOnAction(e -> { stage.close(); com.restaurant.Main.showLogin(); });
        bar.getChildren().addAll(userLbl, spacer, logoutBtn);
        return bar;
    }

    // ── Tab 1: Search & Reserve ──────────────────────────────────────────

    private Tab buildSearchTab() {
        Tab tab = new Tab("Search & Reserve");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);

        Label heading = lbl("Find Available Tables", 18, "#e94560", true);

        HBox searchRow = new HBox(10); searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField capacityField = new TextField(); capacityField.setPromptText("Min guests"); capacityField.setPrefWidth(110);
        DatePicker datePicker = new DatePicker(); datePicker.setPromptText("Date (optional)");
        Spinner<Integer> hourSpin = new Spinner<>(0, 23, 12); hourSpin.setPrefWidth(70);
        Spinner<Integer> minSpin  = new Spinner<>(0, 59,  0, 15); minSpin.setPrefWidth(70);
        Button searchBtn = btn("Search");
        Label searchStatus = new Label(); searchStatus.setStyle("-fx-text-fill: #a8a8b3;");
        searchRow.getChildren().addAll(
                lbl("Capacity:", 13, "#e0e0e0", false), capacityField,
                lbl("Date:", 13, "#e0e0e0", false), datePicker,
                lbl("H:", 13, "#e0e0e0", false), hourSpin,
                lbl("M:", 13, "#e0e0e0", false), minSpin,
                searchBtn, searchStatus);

        TableView<RestaurantTable> tableView = new TableView<>(availableTables);
        tableView.getColumns().addAll(
                col("ID",       "tableId",            60),
                col("Table #",  "tableNumber",        100),
                col("Capacity", "maxCapacity",        100),
                col("Location", "locationIdentifier", 200));
        tableView.setPrefHeight(200);

        Separator sep = new Separator();
        Label resHeading = lbl("Make Reservation", 15, "#e0e0e0", true);

        HBox resRow1 = new HBox(10); resRow1.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Customer> customerBox = new ComboBox<>(customers);
        customerBox.setPromptText("Select customer"); customerBox.setPrefWidth(200);
        TextField peopleField = new TextField("2"); peopleField.setPrefWidth(60);
        resRow1.getChildren().addAll(lbl("Customer:", 13, "#e0e0e0", false), customerBox,
                lbl("Guests:", 13, "#e0e0e0", false), peopleField);

        HBox resRow2 = new HBox(10); resRow2.setAlignment(Pos.CENTER_LEFT);
        DatePicker resDate = new DatePicker();
        Spinner<Integer> resHour = new Spinner<>(0, 23, 12); resHour.setPrefWidth(70);
        Spinner<Integer> resMin  = new Spinner<>(0, 59,  0, 15); resMin.setPrefWidth(70);
        TextField notesField = new TextField(); notesField.setPromptText("Notes (optional)"); notesField.setPrefWidth(180);
        Button reserveBtn = btn("Confirm Reservation");
        Label reserveStatus = new Label(); reserveStatus.setStyle("-fx-text-fill: #4ade80;");
        resRow2.getChildren().addAll(
                lbl("Date:", 13, "#e0e0e0", false), resDate,
                lbl("H:", 13, "#e0e0e0", false), resHour,
                lbl("M:", 13, "#e0e0e0", false), resMin,
                notesField, reserveBtn, reserveStatus);

        searchBtn.setOnAction(e -> {
            int branchId = user.getBranchId() != null ? user.getBranchId() : 1;
            int cap;
            try {
                cap = capacityField.getText().isBlank() ? 1 : Integer.parseInt(capacityField.getText().trim());
            } catch (NumberFormatException ex) { searchStatus.setText("Invalid capacity."); return; }
            searchStatus.setText("Searching…");
            if (datePicker.getValue() != null) {
                LocalDateTime dt = datePicker.getValue().atTime(hourSpin.getValue(), minSpin.getValue());
                int finalCap = cap;
                new Thread(() -> {
                    try {
                        List<RestaurantTable> list = tableDAO.findAvailableAtTime(branchId, finalCap, dt);
                        Platform.runLater(() -> {
                            availableTables.setAll(list);
                            searchStatus.setText("Found " + list.size() + " table(s) free at that time.");
                        });
                    } catch (SQLException ex) {
                        Platform.runLater(() -> searchStatus.setText("Error: " + ex.getMessage()));
                    }
                }).start();
            } else {
                int finalCap = cap;
                new Thread(() -> {
                    try {
                        List<RestaurantTable> list = tableDAO.findAvailableTables(branchId, finalCap);
                        Platform.runLater(() -> {
                            availableTables.setAll(list);
                            searchStatus.setText("Found " + list.size() + " free table(s).");
                        });
                    } catch (SQLException ex) {
                        Platform.runLater(() -> searchStatus.setText("Error: " + ex.getMessage()));
                    }
                }).start();
            }
        });

        reserveBtn.setOnAction(e -> {
            RestaurantTable sel = tableView.getSelectionModel().getSelectedItem();
            if (sel == null)                  { reserveStatus.setText("Select a table."); return; }
            if (customerBox.getValue() == null) { reserveStatus.setText("Select a customer."); return; }
            if (resDate.getValue() == null)     { reserveStatus.setText("Choose a date."); return; }
            int tableId = sel.getTableId();
            int custId  = customerBox.getValue().getCustomerId();
            int guests;
            try { guests = Integer.parseInt(peopleField.getText().trim()); }
            catch (NumberFormatException ex) { reserveStatus.setText("Invalid guest count."); return; }
            LocalDateTime dt    = resDate.getValue().atTime(resHour.getValue(), resMin.getValue());
            String        notes = notesField.getText().trim();
            reserveStatus.setText("Saving…");
            new Thread(() -> {
                try {
                    Reservation res = new Reservation();
                    res.setTableId(tableId);
                    res.setCustomerId(custId);
                    res.setReservedByUserId(user.getUserId());
                    res.setTimeOfReservation(dt);
                    res.setPeopleCount(guests);
                    res.setStatus(ReservationStatus.CONFIRMED);
                    res.setNotes(notes);
                    reservationDAO.insert(res);
                    tableDAO.updateStatus(tableId, TableStatus.RESERVED);
                    Platform.runLater(() -> {
                        reserveStatus.setText("Reservation #" + res.getReservationId() + " confirmed.");
                        availableTables.remove(sel);
                        loadReservations();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> reserveStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(heading, searchRow, tableView, sep, resHeading, resRow1, resRow2);
        tab.setContent(new ScrollPane(root));
        return tab;
    }

    // ── Tab 2: Reservations ──────────────────────────────────────────────

    private Tab buildReservationTab() {
        Tab tab = new Tab("Reservations");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);

        Label heading = lbl("Active Reservations", 18, "#e94560", true);

        HBox searchRow = new HBox(10); searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField(); searchField.setPromptText("Customer name or phone"); searchField.setPrefWidth(260);
        Button searchBtn  = btn("Search");
        Button showAllBtn = btn("Show All");
        Label searchStatus = new Label(); searchStatus.setStyle("-fx-text-fill: #a8a8b3;");
        searchRow.getChildren().addAll(searchField, searchBtn, showAllBtn, searchStatus);

        TableView<Reservation> resView = new TableView<>(reservations);
        resView.getColumns().addAll(
                col("ID",     "reservationId",     60),
                col("Table",  "tableId",           70),
                col("Guests", "peopleCount",        70),
                col("Status", "status",            110),
                col("Time",   "timeOfReservation", 180),
                col("Notes",  "notes",             200));
        resView.setPrefHeight(240);

        HBox actionRow = new HBox(10); actionRow.setAlignment(Pos.CENTER_LEFT);
        Button cancelBtn  = btn("Cancel");
        Button checkinBtn = btn("Check In");
        Button refreshBtn = btn("Refresh");
        Label actionStatus = new Label(); actionStatus.setStyle("-fx-text-fill: #4ade80;");
        actionRow.getChildren().addAll(cancelBtn, checkinBtn, refreshBtn, actionStatus);

        Separator sep = new Separator();
        Label editHeading = lbl("Edit Selected Reservation", 14, "#e0e0e0", true);
        HBox editRow = new HBox(10); editRow.setAlignment(Pos.CENTER_LEFT);
        DatePicker editDate  = new DatePicker();
        Spinner<Integer> editHour = new Spinner<>(0, 23, 12); editHour.setPrefWidth(70);
        Spinner<Integer> editMin  = new Spinner<>(0, 59,  0, 15); editMin.setPrefWidth(70);
        TextField editGuests = new TextField(); editGuests.setPrefWidth(60);
        TextField editNotes  = new TextField(); editNotes.setPrefWidth(200);
        Button saveBtn = btn("Save Changes");
        Label editStatus = new Label(); editStatus.setStyle("-fx-text-fill: #4ade80;");
        editRow.getChildren().addAll(
                lbl("Date:", 13, "#e0e0e0", false), editDate,
                lbl("H:", 13, "#e0e0e0", false), editHour,
                lbl("M:", 13, "#e0e0e0", false), editMin,
                lbl("Guests:", 13, "#e0e0e0", false), editGuests,
                lbl("Notes:", 13, "#e0e0e0", false), editNotes,
                saveBtn, editStatus);

        resView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            if (n.getTimeOfReservation() != null) {
                editDate.setValue(n.getTimeOfReservation().toLocalDate());
                editHour.getValueFactory().setValue(n.getTimeOfReservation().getHour());
                editMin.getValueFactory().setValue(n.getTimeOfReservation().getMinute());
            }
            editGuests.setText(String.valueOf(n.getPeopleCount()));
            editNotes.setText(n.getNotes() != null ? n.getNotes() : "");
        });

        searchBtn.setOnAction(e -> {
            String q = searchField.getText().trim();
            if (q.isBlank()) { searchStatus.setText("Enter a search term."); return; }
            int branchId = user.getBranchId() != null ? user.getBranchId() : 1;
            new Thread(() -> {
                try {
                    List<Reservation> list = reservationDAO.searchByCustomer(q, branchId);
                    Platform.runLater(() -> {
                        reservations.setAll(list);
                        searchStatus.setText("Found " + list.size() + " result(s).");
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> searchStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        showAllBtn.setOnAction(e -> loadReservations());

        cancelBtn.setOnAction(e -> {
            Reservation sel = resView.getSelectionModel().getSelectedItem();
            if (sel == null) { actionStatus.setText("Select a reservation."); return; }
            int resId   = sel.getReservationId();
            int tableId = sel.getTableId();
            new Thread(() -> {
                try {
                    reservationDAO.cancel(resId);
                    tableDAO.updateStatus(tableId, TableStatus.FREE);
                    Platform.runLater(() -> {
                        actionStatus.setText("Reservation #" + resId + " cancelled.");
                        loadReservations();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> actionStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        checkinBtn.setOnAction(e -> {
            Reservation sel = resView.getSelectionModel().getSelectedItem();
            if (sel == null) { actionStatus.setText("Select a reservation."); return; }
            int resId   = sel.getReservationId();
            int tableId = sel.getTableId();
            new Thread(() -> {
                try {
                    reservationDAO.checkIn(resId);
                    tableDAO.updateStatus(tableId, TableStatus.OCCUPIED);
                    Platform.runLater(() -> {
                        actionStatus.setText("Checked in reservation #" + resId + ".");
                        loadReservations();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> actionStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        refreshBtn.setOnAction(e -> loadReservations());

        saveBtn.setOnAction(e -> {
            Reservation sel = resView.getSelectionModel().getSelectedItem();
            if (sel == null)             { editStatus.setText("Select a reservation first."); return; }
            if (editDate.getValue() == null) { editStatus.setText("Choose a date."); return; }
            int guests;
            try { guests = Integer.parseInt(editGuests.getText().trim()); }
            catch (NumberFormatException ex) { editStatus.setText("Invalid guest count."); return; }
            sel.setTimeOfReservation(editDate.getValue().atTime(editHour.getValue(), editMin.getValue()));
            sel.setPeopleCount(guests);
            sel.setNotes(editNotes.getText().trim());
            new Thread(() -> {
                try {
                    reservationDAO.update(sel);
                    Platform.runLater(() -> {
                        editStatus.setText("Reservation #" + sel.getReservationId() + " updated.");
                        loadReservations();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> editStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(heading, searchRow, resView, actionRow, sep, editHeading, editRow);
        tab.setContent(new ScrollPane(root));
        return tab;
    }

    // ── Tab 3: Customers ─────────────────────────────────────────────────

    private Tab buildCustomerTab() {
        Tab tab = new Tab("Customers");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);

        Label heading = lbl("Register New Customer", 18, "#e94560", true);

        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(10);
        TextField nameField  = new TextField(); nameField.setPromptText("Full name");
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone");
        addRow(form, 0, "Full Name:", nameField);
        addRow(form, 1, "Email:",     emailField);
        addRow(form, 2, "Phone:",     phoneField);

        Label status = new Label(); status.setStyle("-fx-text-fill: #4ade80;");
        Button registerBtn = btn("Register Customer");
        registerBtn.setOnAction(e -> {
            if (nameField.getText().isBlank() || phoneField.getText().isBlank()) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Name and phone are required.");
                return;
            }
            String name  = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            new Thread(() -> {
                try {
                    Customer c = new Customer();
                    c.setFullName(name); c.setEmail(email); c.setPhone(phone);
                    customerDAO.insert(c);
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #4ade80;");
                        status.setText("Registered: " + name + " (ID " + c.getCustomerId() + ")");
                        nameField.clear(); emailField.clear(); phoneField.clear();
                        loadCustomers();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #e94560;");
                        status.setText("Error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        Separator sep = new Separator();
        Label listHeading = lbl("All Customers", 14, "#e0e0e0", true);
        TableView<Customer> custView = new TableView<>(customers);
        custView.getColumns().addAll(
                col("ID",    "customerId", 60),
                col("Name",  "fullName",   200),
                col("Phone", "phone",      130),
                col("Email", "email",      200));
        custView.setPrefHeight(220);

        root.getChildren().addAll(heading, form, registerBtn, status, sep, listHeading, custView);
        tab.setContent(root);
        return tab;
    }

    // ── Tab 4: Notifications ─────────────────────────────────────────────

    private Tab buildNotifTab() {
        Tab tab = new Tab("Notifications");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);
        root.getChildren().addAll(
                lbl("Reservation Alerts (background daemon)", 18, "#e94560", true),
                lbl("Alerts appear 30 min before reservation time.", 12, "#a8a8b3", false),
                notifList);
        VBox.setVgrow(notifList, Priority.ALWAYS);
        tab.setContent(root);
        return tab;
    }

    // ── Async loaders ─────────────────────────────────────────────────────

    private void loadReservations() {
        int branchId = user.getBranchId() != null ? user.getBranchId() : 1;
        new Thread(() -> {
            try {
                List<Reservation> list = reservationDAO.findByBranch(branchId);
                Platform.runLater(() -> reservations.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> System.err.println("loadReservations: " + e.getMessage()));
            }
        }).start();
    }

    private void loadCustomers() {
        new Thread(() -> {
            try {
                List<Customer> list = customerDAO.findAll();
                Platform.runLater(() -> customers.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> System.err.println("loadCustomers: " + e.getMessage()));
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

    private void addRow(GridPane grid, int row, String labelText, javafx.scene.Node field) {
        Label l = new Label(labelText); l.setStyle("-fx-text-fill: #e0e0e0;");
        grid.add(l, 0, row); grid.add(field, 1, row);
    }
}
