package com.restaurant.controller;

import com.restaurant.dao.BranchDAO;
import com.restaurant.dao.MenuDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.dao.UserDAO;
import com.restaurant.model.Branch;
import com.restaurant.model.Menu;
import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuSection;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.User;
import com.restaurant.model.enums.TableStatus;
import com.restaurant.model.enums.UserRole;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * Admin Dashboard — allows the ADMIN role to:
 * <ul>
 *   <li>Create / view Restaurant Branches</li>
 *   <li>Create Waiter and Receptionist accounts for a branch</li>
 *   <li>Add Menu Items to a branch menu</li>
 *   <li>Add Tables to a branch</li>
 * </ul>
 */
public class AdminDashboard {

    private static final String DARK_BG   = "-fx-background-color: #1a1a2e;";
    private static final String CARD_BG   = "-fx-background-color: #16213e;";
    private static final String ACCENT    = "-fx-background-color: #e94560;";
    private static final String TEXT_W    = "-fx-text-fill: white;";
    private static final String TEXT_MUTED = "-fx-text-fill: #a8a8b3;";

    private final User admin;
    private final BranchDAO branchDAO   = new BranchDAO();
    private final UserDAO   userDAO     = new UserDAO();
    private final TableDAO  tableDAO    = new TableDAO();
    private final MenuDAO   menuDAO     = new MenuDAO();

    private final Stage stage = new Stage();
    private final ObservableList<Branch> branches = FXCollections.observableArrayList();

    private AdminDashboard(User admin) { this.admin = admin; }

    public static void open(User admin) {
        new AdminDashboard(admin).show();
    }

    private void show() {
        stage.setTitle("Admin Dashboard — " + admin.getUsername());
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle(DARK_BG);
        tabs.getTabs().addAll(
                buildBranchTab(),
                buildEmployeeTab(),
                buildTableTab(),
                buildMenuItemTab()
        );

        BorderPane root = new BorderPane();
        root.setStyle(DARK_BG);
        root.setTop(buildTopBar());
        root.setCenter(tabs);

        Scene scene = new Scene(root, 980, 700);
        stage.setScene(scene);
        stage.show();
        loadBranches();
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #16213e;");

        Label userLabel = new Label("Admin: " + admin.getUsername());
        userLabel.setStyle("-fx-text-fill: #a8a8b3; -fx-font-size: 13;");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            stage.close();
            com.restaurant.Main.showLogin();
        });

        bar.getChildren().addAll(userLabel, spacer, logoutBtn);
        return bar;
    }

    // ── Branch Tab ───────────────────────────────────────────
    private Tab buildBranchTab() {
        Tab tab = new Tab("Branches");

        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);

        Label heading = new Label("Restaurant Branches");
        heading.setFont(Font.font("System", FontWeight.BOLD, 18));
        heading.setStyle("-fx-text-fill: #e94560;");

        TableView<Branch> table = new TableView<>(branches);
        table.setStyle(CARD_BG);
        TableColumn<Branch, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("branchId"));
        TableColumn<Branch, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        TableColumn<Branch, String> locCol = new TableColumn<>("Location");
        locCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locCol.setPrefWidth(300);
        table.getColumns().addAll(idCol, nameCol, locCol);
        table.setPrefHeight(280);

        // Add branch form
        HBox form = new HBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField(); nameField.setPromptText("Branch name");
        TextField locField  = new TextField(); locField.setPromptText("Location / address");
        locField.setPrefWidth(250);
        Button addBtn = styledButton("Add Branch");
        Label status = new Label(); status.setStyle("-fx-text-fill: #4ade80;");

        addBtn.setOnAction(e -> {
            if (nameField.getText().isBlank()) { status.setText("Name required."); return; }
            Branch b = new Branch();
            b.setName(nameField.getText().trim());
            b.setLocation(locField.getText().trim());
            try {
                branchDAO.insert(b);
                loadBranches();
                nameField.clear(); locField.clear();
                status.setText("Branch created (ID " + b.getBranchId() + ")");
            } catch (SQLException ex) {
                status.setText("Error: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(nameField, locField, addBtn, status);
        root.getChildren().addAll(heading, table, new Label("Add new branch:") {{
            setStyle(TEXT_W);}}, form);
        tab.setContent(root);
        return tab;
    }

    // ── Employee Tab ─────────────────────────────────────────
    private Tab buildEmployeeTab() {
        Tab tab = new Tab("Employees");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);

        Label heading = new Label("Create Employee Account");
        heading.setFont(Font.font("System", FontWeight.BOLD, 18));
        heading.setStyle("-fx-text-fill: #e94560;");

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);

        ComboBox<Branch> branchBox = new ComboBox<>(branches);
        branchBox.setPromptText("Select branch");
        branchBox.setPrefWidth(220);

        TextField userField = new TextField(); userField.setPromptText("Username");
        PasswordField passField = new PasswordField(); passField.setPromptText("Password");
        TextField fullNameField = new TextField(); fullNameField.setPromptText("Full name");
        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(UserRole.WAITER, UserRole.RECEPTIONIST);
        roleBox.setPromptText("Role");

        addRow(form, 0, "Branch:", branchBox);
        addRow(form, 1, "Username:", userField);
        addRow(form, 2, "Password:", passField);
        addRow(form, 3, "Full Name:", fullNameField);
        addRow(form, 4, "Role:", roleBox);

        Label status = new Label(); status.setStyle("-fx-text-fill: #4ade80;");
        Button createBtn = styledButton("Create Employee");
        createBtn.setOnAction(e -> {
            if (branchBox.getValue() == null || userField.getText().isBlank()
                    || passField.getText().isBlank() || roleBox.getValue() == null) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("All fields are required.");
                return;
            }
            User u = new User();
            u.setUsername(userField.getText().trim());
            u.setPassword(passField.getText());
            u.setFullName(fullNameField.getText().trim());
            u.setRole(roleBox.getValue());
            u.setBranchId(branchBox.getValue().getBranchId());
            try {
                userDAO.insert(u);
                status.setStyle("-fx-text-fill: #4ade80;");
                status.setText("Account created: " + u.getUsername() + " [" + u.getRole() + "]");
                userField.clear(); passField.clear(); fullNameField.clear();
            } catch (SQLException ex) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Error: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(heading, form, createBtn, status);
        tab.setContent(root);
        return tab;
    }

    // ── Table Tab ────────────────────────────────────────────
    private Tab buildTableTab() {
        Tab tab = new Tab("Tables");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);

        Label heading = new Label("Add Table to Branch");
        heading.setFont(Font.font("System", FontWeight.BOLD, 18));
        heading.setStyle("-fx-text-fill: #e94560;");

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);

        ComboBox<Branch> branchBox = new ComboBox<>(branches);
        branchBox.setPromptText("Select branch");
        TextField tableNumField = new TextField(); tableNumField.setPromptText("e.g. T-01");
        TextField capacityField = new TextField(); capacityField.setPromptText("Max seats");
        TextField locField      = new TextField(); locField.setPromptText("e.g. Window Side");

        addRow(form, 0, "Branch:", branchBox);
        addRow(form, 1, "Table #:", tableNumField);
        addRow(form, 2, "Capacity:", capacityField);
        addRow(form, 3, "Location:", locField);

        Label status = new Label(); status.setStyle("-fx-text-fill: #4ade80;");
        Button addBtn = styledButton("Add Table");
        addBtn.setOnAction(e -> {
            if (branchBox.getValue() == null || tableNumField.getText().isBlank()
                    || capacityField.getText().isBlank()) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Branch, number, and capacity are required.");
                return;
            }
            try {
                int cap = Integer.parseInt(capacityField.getText().trim());
                RestaurantTable t = new RestaurantTable();
                t.setBranchId(branchBox.getValue().getBranchId());
                t.setTableNumber(tableNumField.getText().trim());
                t.setMaxCapacity(cap);
                t.setLocationIdentifier(locField.getText().trim());
                t.setStatus(TableStatus.FREE);
                tableDAO.insert(t);
                status.setStyle("-fx-text-fill: #4ade80;");
                status.setText("Table " + t.getTableNumber() + " added (ID " + t.getTableId()
                        + ") with " + cap + " seats auto-generated.");
                tableNumField.clear(); capacityField.clear(); locField.clear();
            } catch (NumberFormatException nfe) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Capacity must be a number.");
            } catch (SQLException ex) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Error: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(heading, form, addBtn, status);
        tab.setContent(root);
        return tab;
    }

    // ── Menu Item Tab ────────────────────────────────────────
    private Tab buildMenuItemTab() {
        Tab tab = new Tab("Menu Items");
        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle(DARK_BG);

        Label heading = new Label("Add Menu Item");
        heading.setFont(Font.font("System", FontWeight.BOLD, 18));
        heading.setStyle("-fx-text-fill: #e94560;");

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);

        ComboBox<Branch> branchBox = new ComboBox<>(branches);
        branchBox.setPromptText("Select branch");

        TextField sectionField = new TextField(); sectionField.setPromptText("Section name (e.g. Starters)");
        TextField itemNameField = new TextField(); itemNameField.setPromptText("Item name");
        TextField descField     = new TextField(); descField.setPromptText("Description");
        TextField priceField    = new TextField(); priceField.setPromptText("Price (e.g. 12.50)");

        addRow(form, 0, "Branch:", branchBox);
        addRow(form, 1, "Section:", sectionField);
        addRow(form, 2, "Item Name:", itemNameField);
        addRow(form, 3, "Description:", descField);
        addRow(form, 4, "Price ($):", priceField);

        Label status = new Label(); status.setStyle("-fx-text-fill: #4ade80;");
        Button addBtn = styledButton("Add Item");
        addBtn.setOnAction(e -> {
            if (branchBox.getValue() == null || sectionField.getText().isBlank()
                    || itemNameField.getText().isBlank() || priceField.getText().isBlank()) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Branch, section, item name, and price are required.");
                return;
            }
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                Branch branch = branchBox.getValue();
                Menu menu = menuDAO.getOrCreateMenu(branch.getBranchId());

                // Find or create section
                List<MenuSection> sections = menuDAO.findSectionsByMenu(menu.getMenuId());
                MenuSection section = sections.stream()
                        .filter(s -> s.getTitle().equalsIgnoreCase(sectionField.getText().trim()))
                        .findFirst()
                        .orElse(null);
                if (section == null) {
                    section = new MenuSection(0, menu.getMenuId(),
                            sectionField.getText().trim(), "");
                    menuDAO.insertSection(section);
                }

                MenuItem item = new MenuItem(0, section.getSectionId(),
                        itemNameField.getText().trim(), descField.getText().trim(), price);
                menuDAO.insertItem(item);

                status.setStyle("-fx-text-fill: #4ade80;");
                status.setText("Item \"" + item.getTitle() + "\" added to [" + section.getTitle() + "].");
                itemNameField.clear(); descField.clear(); priceField.clear();
            } catch (NumberFormatException nfe) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Price must be a valid number.");
            } catch (SQLException ex) {
                status.setStyle("-fx-text-fill: #e94560;");
                status.setText("Error: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(heading, form, addBtn, status);
        tab.setContent(root);
        return tab;
    }

    // ── Helpers ──────────────────────────────────────────────
    private void loadBranches() {
        try {
            branches.setAll(branchDAO.findAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand;");
        return btn;
    }

    private void addRow(GridPane grid, int row, String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #e0e0e0;");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
    }
}
