package com.restaurant.controller;

import com.restaurant.dao.OrderDAO;
import com.restaurant.model.Meal;
import com.restaurant.model.MealItem;
import com.restaurant.model.Order;
import com.restaurant.model.User;
import com.restaurant.model.enums.OrderStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

public class ChefDashboard {

    private final User     user;
    private final OrderDAO orderDAO = new OrderDAO();

    private final ObservableList<Order>  activeOrders = FXCollections.observableArrayList();
    private final ObservableList<String> orderDetail  = FXCollections.observableArrayList();

    private final Stage stage = new Stage();

    private ChefDashboard(User user) { this.user = user; }

    public static void open(User user) { new ChefDashboard(user).show(); }

    private void show() {
        stage.setTitle("Chef Dashboard — " + user.getUsername());
        stage.setMinWidth(900);
        stage.setMinHeight(620);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #1a1a2e;");
        tabs.getTabs().add(buildKitchenTab());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setTop(buildTopBar());
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 960, 680));
        stage.show();
        loadActiveOrders();
    }

    // ── Top bar ──────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #16213e;");
        Label userLbl = new Label("Chef: " + user.getUsername());
        userLbl.setStyle("-fx-text-fill: #a8a8b3; -fx-font-size: 13;");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutBtn = btn("Logout");
        logoutBtn.setOnAction(e -> { stage.close(); com.restaurant.Main.showLogin(); });
        bar.getChildren().addAll(userLbl, spacer, logoutBtn);
        return bar;
    }

    // ── Kitchen tab ──────────────────────────────────────────────────────

    private Tab buildKitchenTab() {
        Tab tab = new Tab("Kitchen Queue");
        HBox root = new HBox(16);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #1a1a2e;");

        // ── Left: order list ──────────────────────────────────────────────
        VBox leftPane = new VBox(12);
        leftPane.setPrefWidth(440);
        VBox.setVgrow(leftPane, Priority.ALWAYS);

        Label heading = lbl("Active Orders", 18, "#e94560", true);

        TableView<Order> orderView = new TableView<>(activeOrders);
        orderView.getColumns().addAll(
                col("Order #", "orderId",   80),
                col("Table",   "tableId",   80),
                col("Status",  "status",   130),
                col("Placed",  "createdAt",180));
        orderView.setPrefHeight(380);
        VBox.setVgrow(orderView, Priority.ALWAYS);

        Label actionStatus = new Label(); actionStatus.setStyle("-fx-text-fill: #4ade80;");
        Button preparingBtn = btn("Mark Preparing");
        Button completeBtn  = btn("Mark Complete");
        Button refreshBtn   = btn("Refresh");

        HBox actionRow = new HBox(8, preparingBtn, completeBtn, refreshBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        leftPane.getChildren().addAll(heading, orderView, actionRow, actionStatus);

        // ── Right: order details ──────────────────────────────────────────
        VBox rightPane = new VBox(12);
        rightPane.setPrefWidth(440);
        VBox.setVgrow(rightPane, Priority.ALWAYS);

        Label detailHeading = lbl("Order Details", 16, "#e94560", true);
        ListView<String> detailView = new ListView<>(orderDetail);
        detailView.setStyle("-fx-background-color: #16213e; "
                + "-fx-control-inner-background: #16213e; -fx-text-fill: #e0e0e0;");
        VBox.setVgrow(detailView, Priority.ALWAYS);
        rightPane.getChildren().addAll(detailHeading, detailView);

        // ── Wire events ───────────────────────────────────────────────────

        orderView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            orderDetail.clear();
            if (n == null) return;
            int orderId = n.getOrderId();
            new Thread(() -> {
                try {
                    Order refreshed = orderDAO.findById(orderId);
                    List<String> lines = new ArrayList<>();
                    if (refreshed != null) {
                        lines.add("Order #" + refreshed.getOrderId()
                                + "  [" + refreshed.getStatus() + "]");
                        lines.add("Table ID: " + refreshed.getTableId());
                        lines.add("─────────────────────");
                        for (Meal meal : refreshed.getMeals()) {
                            lines.add("  Seat " + meal.getSeatId() + ":");
                            for (MealItem item : meal.getItems()) {
                                lines.add("    " + item.getQuantity()
                                        + "×  " + item.getMenuItemTitle());
                            }
                        }
                    }
                    Platform.runLater(() -> orderDetail.setAll(lines));
                } catch (SQLException ex) {
                    Platform.runLater(() -> {
                        orderDetail.clear();
                        orderDetail.add("Error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        preparingBtn.setOnAction(e -> {
            Order sel = orderView.getSelectionModel().getSelectedItem();
            if (sel == null) { actionStatus.setText("Select an order."); return; }
            if (sel.getStatus() != OrderStatus.RECEIVED) {
                actionStatus.setText("Order must be in RECEIVED state."); return;
            }
            int orderId = sel.getOrderId();
            new Thread(() -> {
                try {
                    orderDAO.updateStatus(orderId, OrderStatus.PREPARING);
                    Platform.runLater(() -> {
                        actionStatus.setText("Order #" + orderId + " → PREPARING");
                        loadActiveOrders();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> actionStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        completeBtn.setOnAction(e -> {
            Order sel = orderView.getSelectionModel().getSelectedItem();
            if (sel == null) { actionStatus.setText("Select an order."); return; }
            if (sel.getStatus() != OrderStatus.PREPARING) {
                actionStatus.setText("Order must be in PREPARING state."); return;
            }
            int orderId = sel.getOrderId();
            new Thread(() -> {
                try {
                    orderDAO.updateStatus(orderId, OrderStatus.COMPLETE);
                    Platform.runLater(() -> {
                        actionStatus.setText("Order #" + orderId + " → COMPLETE");
                        orderDetail.clear();
                        loadActiveOrders();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> actionStatus.setText("Error: " + ex.getMessage()));
                }
            }).start();
        });

        refreshBtn.setOnAction(e -> loadActiveOrders());

        root.getChildren().addAll(leftPane, rightPane);
        tab.setContent(root);
        return tab;
    }

    // ── Async loaders ─────────────────────────────────────────────────────

    private void loadActiveOrders() {
        int branchId = user.getBranchId() != null ? user.getBranchId() : 1;
        new Thread(() -> {
            try {
                List<Order> list = orderDAO.findActiveByBranch(branchId);
                Platform.runLater(() -> activeOrders.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> System.err.println("loadActiveOrders: " + e.getMessage()));
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
