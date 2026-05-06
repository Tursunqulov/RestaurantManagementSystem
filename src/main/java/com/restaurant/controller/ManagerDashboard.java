package com.restaurant.controller;

import com.restaurant.dao.BranchDAO;
import com.restaurant.dao.MenuDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.dao.UserDAO;
import com.restaurant.model.Branch;
import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuSection;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.User;
import com.restaurant.model.enums.TableStatus;
import com.restaurant.model.enums.UserRole;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
 * Manager Dashboard — full CRUD over workers, branches, tables, and menus.
 *
 * CRITICAL: every database call runs on a background Thread.
 * Every UI update runs inside Platform.runLater().
 */
public class ManagerDashboard {

    private static final String DARK_BG = "-fx-background-color: #1a1a2e;";
    private static final String CARD_BG = "-fx-background-color: #16213e;";
    private static final String ACCENT  = "#e94560";

    private final User       manager;
    private final UserDAO    userDAO    = new UserDAO();
    private final BranchDAO  branchDAO  = new BranchDAO();
    private final TableDAO   tableDAO   = new TableDAO();
    private final MenuDAO    menuDAO    = new MenuDAO();

    // Shared lists — branches is referenced by every tab's ComboBox
    private final ObservableList<Branch>          branches = FXCollections.observableArrayList();
    private final ObservableList<User>            workers  = FXCollections.observableArrayList();
    private final ObservableList<RestaurantTable> tables   = FXCollections.observableArrayList();
    private final ObservableList<MenuSection>     sections = FXCollections.observableArrayList();
    private final ObservableList<MenuItem>        items    = FXCollections.observableArrayList();

    private final Stage stage = new Stage();

    private ManagerDashboard(User manager) { this.manager = manager; }

    public static void open(User manager) {
        new ManagerDashboard(manager).show();
    }

    // ── Window setup ─────────────────────────────────────────

    private void show() {
        stage.setTitle("Manager Dashboard — " + manager.getUsername());
        stage.setMinWidth(1100);
        stage.setMinHeight(720);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle(DARK_BG);
        tabs.getTabs().addAll(
                buildWorkersTab(),
                buildBranchesTab(),
                buildTablesTab(),
                buildMenuTab(),
                buildAddEmployeeTab()
        );

        BorderPane root = new BorderPane();
        root.setStyle(DARK_BG);
        root.setTop(buildTopBar());
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 1140, 760));
        stage.show();

        // Prime the shared branches list so every tab's ComboBox is populated
        loadBranchesAsync(new Label());
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(CARD_BG);

        Label who = new Label("Manager: " + manager.getUsername());
        who.setStyle("-fx-text-fill: #a8a8b3; -fx-font-size: 13;");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = styledBtn("Logout");
        logoutBtn.setOnAction(e -> { stage.close(); com.restaurant.Main.showLogin(); });

        bar.getChildren().addAll(who, spacer, logoutBtn);
        return bar;
    }

    // ══════════════ TAB 1 — WORKERS ══════════════════════════

    private Tab buildWorkersTab() {
        Tab tab = new Tab("Workers");
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle(DARK_BG);

        Label heading   = label("Worker Management", 18, ACCENT, true);
        Label tabStatus = new Label();
        tabStatus.setStyle("-fx-text-fill: #4ade80;");

        // Filter bar
        ComboBox<Branch> filterBranch = new ComboBox<>(branches);
        filterBranch.setPromptText("All Branches");
        filterBranch.setPrefWidth(220);
        Button loadBtn = styledBtn("Load Workers");
        HBox filterBar = new HBox(10,
                label("Branch:", 13, "#e0e0e0", false), filterBranch, loadBtn, tabStatus);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // Workers table
        TableView<User> workerTable = new TableView<>(workers);
        workerTable.setStyle(CARD_BG);
        workerTable.getColumns().addAll(
                col("ID",        "userId",   50),
                col("Username",  "username", 130),
                col("Full Name", "fullName", 160),
                col("Role",      "role",     110),
                col("Branch",    "branchId", 75),
                col("Active",    "active",   65)
        );
        VBox.setVgrow(workerTable, Priority.ALWAYS);

        // Edit panel (right side)
        TextField     editName   = new TextField();
        ComboBox<UserRole> editRole = new ComboBox<>();
        editRole.getItems().addAll(
                UserRole.RECEPTIONIST, UserRole.WAITER,
                UserRole.CASHIER, UserRole.CHEF, UserRole.MANAGER);
        editRole.setPrefWidth(180);
        ComboBox<Branch> editBranch = new ComboBox<>(branches);
        editBranch.setPrefWidth(180);
        CheckBox editActive = new CheckBox("Active");
        editActive.setStyle("-fx-text-fill: #e0e0e0;");
        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("New password");
        Label editStatus = new Label();
        editStatus.setStyle("-fx-text-fill: #a8a8b3;");
        editStatus.setWrapText(true);

        Button saveBtn   = styledBtn("Save Changes");
        Button setPwdBtn = styledBtn("Set Password");
        Button deleteBtn = dangerBtn("Delete Worker");

        GridPane editForm = new GridPane();
        editForm.setHgap(8); editForm.setVgap(8);
        formRow(editForm, 0, "Full Name:", editName);
        formRow(editForm, 1, "Role:",      editRole);
        formRow(editForm, 2, "Branch:",    editBranch);
        editForm.add(editActive, 1, 3);

        VBox editPanel = new VBox(10,
                label("Edit Selected Worker", 14, "#e0e0e0", true),
                editForm, saveBtn,
                new Separator(),
                label("Change Password", 13, "#e0e0e0", true),
                newPassField, setPwdBtn,
                new Separator(),
                deleteBtn, editStatus);
        editPanel.setPadding(new Insets(12));
        editPanel.setStyle(CARD_BG);
        editPanel.setPrefWidth(272);
        editPanel.setMinWidth(272);

        // Populate edit panel when a row is selected
        workerTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            editName.setText(sel.getFullName() != null ? sel.getFullName() : "");
            editRole.setValue(sel.getRole());
            branches.stream()
                    .filter(b -> b.getBranchId() ==
                            (sel.getBranchId() != null ? sel.getBranchId() : -1))
                    .findFirst().ifPresent(editBranch::setValue);
            editActive.setSelected(sel.isActive());
            editStatus.setStyle("-fx-text-fill: #a8a8b3;");
            editStatus.setText("Editing: " + sel.getUsername());
        });

        // Save changes
        saveBtn.setOnAction(e -> {
            User sel = workerTable.getSelectionModel().getSelectedItem();
            if (sel == null) { setErr(editStatus, "Select a worker first."); return; }
            sel.setFullName(editName.getText().trim());
            if (editRole.getValue() != null) sel.setRole(editRole.getValue());
            sel.setBranchId(editBranch.getValue() != null
                    ? editBranch.getValue().getBranchId() : null);
            sel.setActive(editActive.isSelected());
            new Thread(() -> {
                try {
                    userDAO.update(sel);
                    Platform.runLater(() -> {
                        setOk(editStatus, "Saved: " + sel.getUsername());
                        loadWorkersAsync(filterBranch.getValue(), tabStatus);
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(editStatus, ex.getMessage()));
                }
            }).start();
        });

        // Change password
        setPwdBtn.setOnAction(e -> {
            User sel = workerTable.getSelectionModel().getSelectedItem();
            if (sel == null)              { setErr(editStatus, "Select a worker first."); return; }
            if (newPassField.getText().isBlank()) { setErr(editStatus, "Enter a password."); return; }
            String pwd = newPassField.getText();
            new Thread(() -> {
                try {
                    userDAO.changePassword(sel.getUserId(), pwd);
                    Platform.runLater(() -> {
                        setOk(editStatus, "Password updated for " + sel.getUsername());
                        newPassField.clear();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(editStatus, ex.getMessage()));
                }
            }).start();
        });

        // Delete with confirmation
        deleteBtn.setOnAction(e -> {
            User sel = workerTable.getSelectionModel().getSelectedItem();
            if (sel == null) { setErr(editStatus, "Select a worker first."); return; }
            confirm("Delete worker \"" + sel.getUsername() + "\"? This cannot be undone.")
                    .ifPresent(bt -> {
                        if (bt != ButtonType.OK) return;
                        new Thread(() -> {
                            try {
                                userDAO.delete(sel.getUserId());
                                Platform.runLater(() -> {
                                    workers.remove(sel);
                                    setOk(editStatus, "Deleted: " + sel.getUsername());
                                });
                            } catch (SQLException ex) {
                                Platform.runLater(() -> setErr(editStatus, ex.getMessage()));
                            }
                        }).start();
                    });
        });

        loadBtn.setOnAction(e -> loadWorkersAsync(filterBranch.getValue(), tabStatus));

        HBox content = new HBox(10, workerTable, editPanel);
        HBox.setHgrow(workerTable, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);

        root.getChildren().addAll(heading, filterBar, content);
        tab.setContent(root);
        return tab;
    }

    // ══════════════ TAB 2 — BRANCHES ═════════════════════════

    private Tab buildBranchesTab() {
        Tab tab = new Tab("Branches");
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle(DARK_BG);

        Label heading   = label("Branch Management", 18, ACCENT, true);
        Label tabStatus = new Label(); tabStatus.setStyle("-fx-text-fill: #4ade80;");

        // Separate list for this table — may include inactive rows.
        // The shared `branches` list (used by all ComboBoxes) stays active-only.
        ObservableList<Branch> displayBranches = FXCollections.observableArrayList();

        TableView<Branch> branchTable = new TableView<>(displayBranches);
        branchTable.setStyle(CARD_BG);
        branchTable.getColumns().addAll(
                col("ID",       "branchId", 50),
                col("Name",     "name",     200),
                col("Location", "location", 260),
                col("Active",   "active",   65)
        );
        branchTable.setPrefHeight(260);

        CheckBox showInactiveChk = new CheckBox("Show Inactive");
        showInactiveChk.setStyle("-fx-text-fill: #e0e0e0;");

        Button deactivateBtn  = styledBtn("Deactivate");
        Button reactivateBtn  = styledBtn("Reactivate");
        Button deleteBtn      = dangerBtn("Delete Branch");
        Button refreshBtn     = styledBtn("Refresh");

        // Reloads only the display list, respecting the "Show Inactive" toggle.
        // Also refreshes the shared `branches` list (active-only) for ComboBoxes.
        Runnable reloadDisplay = () -> {
            boolean incl = showInactiveChk.isSelected();
            new Thread(() -> {
                try {
                    List<Branch> display = branchDAO.findAll(incl);
                    List<Branch> active  = incl ? branchDAO.findAll(false) : display;
                    Platform.runLater(() -> {
                        displayBranches.setAll(display);
                        branches.setAll(active);
                        tabStatus.setStyle("-fx-text-fill: #a8a8b3;");
                        tabStatus.setText(display.size() + " branch(es)"
                                + (incl ? " (including inactive)" : "") + ".");
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(tabStatus, ex.getMessage()));
                }
            }).start();
        };

        showInactiveChk.setOnAction(e -> reloadDisplay.run());
        refreshBtn.setOnAction(e -> reloadDisplay.run());

        deactivateBtn.setOnAction(e -> {
            Branch sel = branchTable.getSelectionModel().getSelectedItem();
            if (sel == null)       { setErr(tabStatus, "Select a branch first."); return; }
            if (!sel.isActive())   { setErr(tabStatus, "Already inactive."); return; }
            new Thread(() -> {
                try {
                    branchDAO.setActive(sel.getBranchId(), false);
                    Platform.runLater(() -> {
                        setOk(tabStatus, "Deactivated: " + sel.getName());
                        reloadDisplay.run();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(tabStatus, ex.getMessage()));
                }
            }).start();
        });

        reactivateBtn.setOnAction(e -> {
            Branch sel = branchTable.getSelectionModel().getSelectedItem();
            if (sel == null)     { setErr(tabStatus, "Select a branch first."); return; }
            if (sel.isActive())  { setErr(tabStatus, "Branch is already active."); return; }
            new Thread(() -> {
                try {
                    branchDAO.setActive(sel.getBranchId(), true);
                    Platform.runLater(() -> {
                        setOk(tabStatus, "Reactivated: " + sel.getName());
                        reloadDisplay.run();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(tabStatus, ex.getMessage()));
                }
            }).start();
        });

        deleteBtn.setOnAction(e -> {
            Branch sel = branchTable.getSelectionModel().getSelectedItem();
            if (sel == null) { setErr(tabStatus, "Select a branch first."); return; }
            confirm("Permanently delete branch \"" + sel.getName() + "\"? This cannot be undone.")
                    .ifPresent(bt -> {
                        if (bt != ButtonType.OK) return;
                        new Thread(() -> {
                            try {
                                branchDAO.delete(sel.getBranchId());
                                Platform.runLater(() -> {
                                    setOk(tabStatus, "Deleted: " + sel.getName());
                                    reloadDisplay.run();
                                });
                            } catch (SQLException ex) {
                                Platform.runLater(() -> setErr(tabStatus, ex.getMessage()));
                            }
                        }).start();
                    });
        });

        HBox actionBar = new HBox(10, deactivateBtn, reactivateBtn, deleteBtn,
                refreshBtn, showInactiveChk, tabStatus);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        // Add branch form
        TextField nameField = new TextField(); nameField.setPromptText("Branch name");
        TextField locField  = new TextField(); locField.setPromptText("Address / location");
        locField.setPrefWidth(250);
        Label addStatus = new Label(); addStatus.setStyle("-fx-text-fill: #4ade80;");
        Button addBtn = styledBtn("Add Branch");

        addBtn.setOnAction(e -> {
            if (nameField.getText().isBlank()) { setErr(addStatus, "Name required."); return; }
            Branch b = new Branch();
            b.setName(nameField.getText().trim());
            b.setLocation(locField.getText().trim());
            new Thread(() -> {
                try {
                    branchDAO.insert(b);
                    Platform.runLater(() -> {
                        setOk(addStatus, "Created: " + b.getName() + " (ID " + b.getBranchId() + ")");
                        nameField.clear(); locField.clear();
                        reloadDisplay.run();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(addStatus, ex.getMessage()));
                }
            }).start();
        });

        HBox addForm = new HBox(10, nameField, locField, addBtn, addStatus);
        addForm.setAlignment(Pos.CENTER_LEFT);

        // Initial load
        reloadDisplay.run();

        root.getChildren().addAll(heading, branchTable, actionBar,
                new Separator(), label("Add New Branch", 14, "#e0e0e0", true), addForm);
        tab.setContent(root);
        return tab;
    }

    // ══════════════ TAB 3 — TABLES ═══════════════════════════

    private Tab buildTablesTab() {
        Tab tab = new Tab("Tables");
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle(DARK_BG);

        Label heading   = label("Table Management", 18, ACCENT, true);
        Label tabStatus = new Label(); tabStatus.setStyle("-fx-text-fill: #4ade80;");

        ComboBox<Branch> branchSel = new ComboBox<>(branches);
        branchSel.setPromptText("Select branch"); branchSel.setPrefWidth(220);
        Button loadBtn = styledBtn("Load Tables");
        HBox filterBar = new HBox(10,
                label("Branch:", 13, "#e0e0e0", false), branchSel, loadBtn, tabStatus);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TableView<RestaurantTable> tableView = new TableView<>(tables);
        tableView.setStyle(CARD_BG);
        tableView.getColumns().addAll(
                col("ID",       "tableId",            55),
                col("Table #",  "tableNumber",        100),
                col("Capacity", "maxCapacity",        90),
                col("Location", "locationIdentifier", 200),
                col("Status",   "status",             110)
        );
        VBox.setVgrow(tableView, Priority.ALWAYS);

        Button deleteBtn  = dangerBtn("Delete Selected");
        Button refreshBtn = styledBtn("Refresh");

        deleteBtn.setOnAction(e -> {
            RestaurantTable sel = tableView.getSelectionModel().getSelectedItem();
            if (sel == null) { setErr(tabStatus, "Select a table first."); return; }
            confirm("Delete table " + sel.getTableNumber() + "? All its seats will also be removed.")
                    .ifPresent(bt -> {
                        if (bt != ButtonType.OK) return;
                        new Thread(() -> {
                            try {
                                tableDAO.delete(sel.getTableId());
                                Platform.runLater(() -> {
                                    tables.remove(sel);
                                    setOk(tabStatus, "Deleted table " + sel.getTableNumber());
                                });
                            } catch (SQLException ex) {
                                Platform.runLater(() -> setErr(tabStatus, ex.getMessage()));
                            }
                        }).start();
                    });
        });

        loadBtn.setOnAction(e -> {
            if (branchSel.getValue() != null)
                loadTablesAsync(branchSel.getValue().getBranchId(), tabStatus);
            else setErr(tabStatus, "Select a branch.");
        });
        refreshBtn.setOnAction(e -> {
            if (branchSel.getValue() != null)
                loadTablesAsync(branchSel.getValue().getBranchId(), tabStatus);
        });

        HBox actionBar = new HBox(10, deleteBtn, refreshBtn);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        // Add table form
        GridPane form = new GridPane(); form.setHgap(10); form.setVgap(8);
        TextField numField = new TextField(); numField.setPromptText("e.g. T-01");
        TextField capField = new TextField(); capField.setPromptText("Seats");
        TextField locFld   = new TextField(); locFld.setPromptText("e.g. Window Side");
        formRow(form, 0, "Table #:",   numField);
        formRow(form, 1, "Capacity:",  capField);
        formRow(form, 2, "Location:",  locFld);

        Label addStatus = new Label(); addStatus.setStyle("-fx-text-fill: #4ade80;");
        Button addBtn = styledBtn("Add Table");
        addBtn.setOnAction(e -> {
            Branch branch = branchSel.getValue();
            if (branch == null) { setErr(addStatus, "Select a branch first."); return; }
            if (numField.getText().isBlank() || capField.getText().isBlank()) {
                setErr(addStatus, "Table # and capacity required."); return;
            }
            try {
                int cap = Integer.parseInt(capField.getText().trim());
                RestaurantTable t = new RestaurantTable();
                t.setBranchId(branch.getBranchId());
                t.setTableNumber(numField.getText().trim());
                t.setMaxCapacity(cap);
                t.setLocationIdentifier(locFld.getText().trim());
                t.setStatus(TableStatus.FREE);
                new Thread(() -> {
                    try {
                        tableDAO.insert(t);
                        Platform.runLater(() -> {
                            setOk(addStatus, "Added: " + t.getTableNumber()
                                    + " (ID " + t.getTableId() + ", " + cap + " seats)");
                            numField.clear(); capField.clear(); locFld.clear();
                            loadTablesAsync(branch.getBranchId(), tabStatus);
                        });
                    } catch (SQLException ex) {
                        Platform.runLater(() -> setErr(addStatus, ex.getMessage()));
                    }
                }).start();
            } catch (NumberFormatException nfe) {
                setErr(addStatus, "Capacity must be a number.");
            }
        });

        root.getChildren().addAll(heading, filterBar, tableView, actionBar,
                new Separator(), label("Add Table", 14, "#e0e0e0", true),
                form, addBtn, addStatus);
        tab.setContent(root);
        return tab;
    }

    // ══════════════ TAB 4 — MENU ═════════════════════════════

    private Tab buildMenuTab() {
        Tab tab = new Tab("Menu");
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle(DARK_BG);

        Label heading      = label("Menu Management", 18, ACCENT, true);
        Label globalStatus = new Label(); globalStatus.setStyle("-fx-text-fill: #4ade80;");

        ComboBox<Branch> branchSel = new ComboBox<>(branches);
        branchSel.setPromptText("Select branch"); branchSel.setPrefWidth(220);
        Button loadMenuBtn = styledBtn("Load Menu");
        HBox branchBar = new HBox(10,
                label("Branch:", 13, "#e0e0e0", false), branchSel, loadMenuBtn, globalStatus);
        branchBar.setAlignment(Pos.CENTER_LEFT);

        // ── Sections panel (left) ──────────────────────────
        ListView<MenuSection> sectionList = new ListView<>(sections);
        sectionList.setPrefHeight(200);
        sectionList.setStyle(CARD_BG);

        TextField secTitle = new TextField(); secTitle.setPromptText("Section title");
        TextField secDesc  = new TextField(); secDesc.setPromptText("Description");
        Label secStatus = new Label(); secStatus.setStyle("-fx-text-fill: #4ade80;");

        Button addSecBtn  = styledBtn("Add");
        Button saveSecBtn = styledBtn("Save Edit");
        Button delSecBtn  = dangerBtn("Delete");

        // Populate fields + load items when section is selected
        sectionList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            secTitle.setText(sel.getTitle());
            secDesc.setText(sel.getDescription() != null ? sel.getDescription() : "");
            items.clear();
            loadItemsAsync(sel.getSectionId(), globalStatus);
        });

        addSecBtn.setOnAction(e -> {
            Branch branch = branchSel.getValue();
            if (branch == null) { setErr(secStatus, "Select branch first."); return; }
            if (secTitle.getText().isBlank()) { setErr(secStatus, "Title required."); return; }
            new Thread(() -> {
                try {
                    com.restaurant.model.Menu menu = menuDAO.getOrCreateMenu(branch.getBranchId());
                    MenuSection sec = new MenuSection(0, menu.getMenuId(),
                            secTitle.getText().trim(), secDesc.getText().trim());
                    menuDAO.insertSection(sec);
                    Platform.runLater(() -> {
                        setOk(secStatus, "Added: " + sec.getTitle());
                        secTitle.clear(); secDesc.clear();
                        loadSectionsAsync(branch.getBranchId(), globalStatus);
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(secStatus, ex.getMessage()));
                }
            }).start();
        });

        saveSecBtn.setOnAction(e -> {
            MenuSection sel = sectionList.getSelectionModel().getSelectedItem();
            if (sel == null) { setErr(secStatus, "Select section first."); return; }
            if (secTitle.getText().isBlank()) { setErr(secStatus, "Title required."); return; }
            sel.setTitle(secTitle.getText().trim());
            sel.setDescription(secDesc.getText().trim());
            new Thread(() -> {
                try {
                    menuDAO.updateSection(sel);
                    Platform.runLater(() -> {
                        setOk(secStatus, "Updated: " + sel.getTitle());
                        if (branchSel.getValue() != null)
                            loadSectionsAsync(branchSel.getValue().getBranchId(), globalStatus);
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(secStatus, ex.getMessage()));
                }
            }).start();
        });

        delSecBtn.setOnAction(e -> {
            MenuSection sel = sectionList.getSelectionModel().getSelectedItem();
            if (sel == null) { setErr(secStatus, "Select section first."); return; }
            confirm("Delete section \"" + sel.getTitle() + "\" and ALL its items?")
                    .ifPresent(bt -> {
                        if (bt != ButtonType.OK) return;
                        new Thread(() -> {
                            try {
                                menuDAO.deleteSection(sel.getSectionId());
                                Platform.runLater(() -> {
                                    sections.remove(sel);
                                    items.clear();
                                    setOk(secStatus, "Deleted: " + sel.getTitle());
                                });
                            } catch (SQLException ex) {
                                Platform.runLater(() -> setErr(secStatus, ex.getMessage()));
                            }
                        }).start();
                    });
        });

        VBox secPanel = new VBox(8,
                label("Sections", 14, "#e0e0e0", true),
                sectionList,
                label("Title:", 12, "#a8a8b3", false), secTitle,
                label("Description:", 12, "#a8a8b3", false), secDesc,
                new HBox(6, addSecBtn, saveSecBtn, delSecBtn),
                secStatus);
        secPanel.setPrefWidth(280); secPanel.setMinWidth(280);
        secPanel.setPadding(new Insets(10));
        secPanel.setStyle(CARD_BG);

        // ── Items panel (right) ───────────────────────────
        TableView<MenuItem> itemTable = new TableView<>(items);
        itemTable.setStyle(CARD_BG);
        itemTable.getColumns().addAll(
                col("ID",    "itemId",    50),
                col("Title", "title",     180),
                col("Price", "price",     80),
                col("Avail", "available", 55)
        );
        itemTable.setPrefHeight(180);

        TextField itemTitle = new TextField(); itemTitle.setPromptText("Item title");
        TextField itemDesc  = new TextField(); itemDesc.setPromptText("Description");
        TextField itemPrice = new TextField(); itemPrice.setPromptText("Price e.g. 9.99");
        CheckBox  itemAvail = new CheckBox("Available");
        itemAvail.setSelected(true); itemAvail.setStyle("-fx-text-fill: #e0e0e0;");
        Label itemStatus = new Label(); itemStatus.setStyle("-fx-text-fill: #4ade80;");

        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            itemTitle.setText(sel.getTitle());
            itemDesc.setText(sel.getDescription() != null ? sel.getDescription() : "");
            itemPrice.setText(String.valueOf(sel.getPrice()));
            itemAvail.setSelected(sel.isAvailable());
        });

        Button addItemBtn  = styledBtn("Add Item");
        Button saveItemBtn = styledBtn("Save Edit");
        Button delItemBtn  = dangerBtn("Delete");

        addItemBtn.setOnAction(e -> {
            MenuSection secSel = sectionList.getSelectionModel().getSelectedItem();
            if (secSel == null) { setErr(itemStatus, "Select a section first."); return; }
            if (itemTitle.getText().isBlank() || itemPrice.getText().isBlank()) {
                setErr(itemStatus, "Title and price required."); return;
            }
            try {
                double price = Double.parseDouble(itemPrice.getText().trim());
                MenuItem item = new MenuItem(0, secSel.getSectionId(),
                        itemTitle.getText().trim(), itemDesc.getText().trim(), price);
                item.setAvailable(itemAvail.isSelected());
                new Thread(() -> {
                    try {
                        menuDAO.insertItem(item);
                        Platform.runLater(() -> {
                            setOk(itemStatus, "Added: " + item.getTitle());
                            itemTitle.clear(); itemDesc.clear(); itemPrice.clear();
                            loadItemsAsync(secSel.getSectionId(), globalStatus);
                        });
                    } catch (SQLException ex) {
                        Platform.runLater(() -> setErr(itemStatus, ex.getMessage()));
                    }
                }).start();
            } catch (NumberFormatException nfe) {
                setErr(itemStatus, "Invalid price.");
            }
        });

        saveItemBtn.setOnAction(e -> {
            MenuItem selItem = itemTable.getSelectionModel().getSelectedItem();
            if (selItem == null) { setErr(itemStatus, "Select an item first."); return; }
            if (itemTitle.getText().isBlank() || itemPrice.getText().isBlank()) {
                setErr(itemStatus, "Title and price required."); return;
            }
            try {
                selItem.setTitle(itemTitle.getText().trim());
                selItem.setDescription(itemDesc.getText().trim());
                selItem.setPrice(Double.parseDouble(itemPrice.getText().trim()));
                selItem.setAvailable(itemAvail.isSelected());
                new Thread(() -> {
                    try {
                        menuDAO.updateItem(selItem);
                        Platform.runLater(() -> {
                            setOk(itemStatus, "Updated: " + selItem.getTitle());
                            MenuSection secSel = sectionList.getSelectionModel().getSelectedItem();
                            if (secSel != null) loadItemsAsync(secSel.getSectionId(), globalStatus);
                        });
                    } catch (SQLException ex) {
                        Platform.runLater(() -> setErr(itemStatus, ex.getMessage()));
                    }
                }).start();
            } catch (NumberFormatException nfe) {
                setErr(itemStatus, "Invalid price.");
            }
        });

        delItemBtn.setOnAction(e -> {
            MenuItem selItem = itemTable.getSelectionModel().getSelectedItem();
            if (selItem == null) { setErr(itemStatus, "Select an item."); return; }
            new Thread(() -> {
                try {
                    menuDAO.deleteItem(selItem.getItemId());
                    Platform.runLater(() -> {
                        items.remove(selItem);
                        setOk(itemStatus, "Deleted: " + selItem.getTitle());
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(itemStatus, ex.getMessage()));
                }
            }).start();
        });

        GridPane itemForm = new GridPane(); itemForm.setHgap(8); itemForm.setVgap(6);
        formRow(itemForm, 0, "Title:",    itemTitle);
        formRow(itemForm, 1, "Desc:",     itemDesc);
        formRow(itemForm, 2, "Price ($):", itemPrice);
        itemForm.add(itemAvail, 1, 3);

        VBox itemPanel = new VBox(8,
                label("Menu Items", 14, "#e0e0e0", true),
                itemTable, itemForm,
                new HBox(6, addItemBtn, saveItemBtn, delItemBtn),
                itemStatus);
        itemPanel.setPadding(new Insets(10));
        itemPanel.setStyle(CARD_BG);
        HBox.setHgrow(itemPanel, Priority.ALWAYS);

        HBox menuContent = new HBox(10, secPanel, itemPanel);
        VBox.setVgrow(menuContent, Priority.ALWAYS);

        loadMenuBtn.setOnAction(e -> {
            Branch branch = branchSel.getValue();
            if (branch == null) { setErr(globalStatus, "Select a branch."); return; }
            loadSectionsAsync(branch.getBranchId(), globalStatus);
        });

        root.getChildren().addAll(heading, branchBar, menuContent);
        tab.setContent(root);
        return tab;
    }

    // ══════════════ TAB 5 — ADD EMPLOYEE ═════════════════════

    private Tab buildAddEmployeeTab() {
        Tab tab = new Tab("Add Employee");
        VBox root = new VBox(14);
        root.setPadding(new Insets(16));
        root.setStyle(DARK_BG);

        Label heading = label("Create Employee Account", 18, ACCENT, true);

        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(10);
        ComboBox<Branch>   branchBox = new ComboBox<>(branches);
        branchBox.setPromptText("Select branch"); branchBox.setPrefWidth(220);
        TextField   userField = new TextField();  userField.setPromptText("Username");
        PasswordField passField = new PasswordField(); passField.setPromptText("Password");
        TextField   nameField = new TextField();  nameField.setPromptText("Full name");
        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(UserRole.RECEPTIONIST, UserRole.WAITER,
                                  UserRole.CASHIER, UserRole.CHEF, UserRole.MANAGER);
        roleBox.setPromptText("Role");

        formRow(form, 0, "Branch:",    branchBox);
        formRow(form, 1, "Username:",  userField);
        formRow(form, 2, "Password:",  passField);
        formRow(form, 3, "Full Name:", nameField);
        formRow(form, 4, "Role:",      roleBox);

        Label status = new Label(); status.setStyle("-fx-text-fill: #4ade80;");
        Button createBtn = styledBtn("Create Employee");
        createBtn.setOnAction(e -> {
            if (branchBox.getValue() == null || userField.getText().isBlank()
                    || passField.getText().isBlank() || roleBox.getValue() == null) {
                setErr(status, "All fields are required."); return;
            }
            User u = new User();
            u.setUsername(userField.getText().trim());
            u.setPassword(passField.getText());
            u.setFullName(nameField.getText().trim());
            u.setRole(roleBox.getValue());
            u.setBranchId(branchBox.getValue().getBranchId());
            new Thread(() -> {
                try {
                    userDAO.insert(u);
                    Platform.runLater(() -> {
                        setOk(status, "Created: " + u.getUsername() + " [" + u.getRole() + "]");
                        userField.clear(); passField.clear(); nameField.clear();
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() -> setErr(status, ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(heading, form, createBtn, status);
        tab.setContent(root);
        return tab;
    }

    // ══════════════ ASYNC LOADERS ═════════════════════════════

    private void loadBranchesAsync(Label status) {
        new Thread(() -> {
            try {
                List<Branch> list = branchDAO.findAll();
                Platform.runLater(() -> {
                    branches.setAll(list);
                    status.setStyle("-fx-text-fill: #a8a8b3;");
                    status.setText(list.size() + " branch(es) loaded.");
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> setErr(status, ex.getMessage()));
            }
        }).start();
    }

    private void loadWorkersAsync(Branch branch, Label status) {
        new Thread(() -> {
            try {
                Integer bid = branch != null ? branch.getBranchId() : null;
                List<User> list = userDAO.findAll(bid);
                Platform.runLater(() -> {
                    workers.setAll(list);
                    status.setStyle("-fx-text-fill: #a8a8b3;");
                    status.setText("Loaded " + list.size() + " worker(s).");
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> setErr(status, ex.getMessage()));
            }
        }).start();
    }

    private void loadTablesAsync(int branchId, Label status) {
        new Thread(() -> {
            try {
                List<RestaurantTable> list = tableDAO.findByBranch(branchId);
                Platform.runLater(() -> {
                    tables.setAll(list);
                    status.setStyle("-fx-text-fill: #a8a8b3;");
                    status.setText("Loaded " + list.size() + " table(s).");
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> setErr(status, ex.getMessage()));
            }
        }).start();
    }

    private void loadSectionsAsync(int branchId, Label status) {
        new Thread(() -> {
            try {
                com.restaurant.model.Menu menu = menuDAO.findMenuByBranch(branchId);
                if (menu == null) {
                    Platform.runLater(() -> {
                        sections.clear();
                        items.clear();
                        status.setStyle("-fx-text-fill: #a8a8b3;");
                        status.setText("No menu yet — add a section to create one.");
                    });
                    return;
                }
                List<MenuSection> list = menuDAO.findSectionsByMenu(menu.getMenuId());
                Platform.runLater(() -> {
                    sections.setAll(list);
                    items.clear();
                    status.setStyle("-fx-text-fill: #a8a8b3;");
                    status.setText("Loaded " + list.size() + " section(s).");
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> setErr(status, ex.getMessage()));
            }
        }).start();
    }

    private void loadItemsAsync(int sectionId, Label status) {
        new Thread(() -> {
            try {
                List<MenuItem> list = menuDAO.findItemsBySection(sectionId);
                Platform.runLater(() -> {
                    items.setAll(list);
                    status.setStyle("-fx-text-fill: #a8a8b3;");
                    status.setText("Loaded " + list.size() + " item(s).");
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> setErr(status, ex.getMessage()));
            }
        }).start();
    }

    // ══════════════ HELPERS ═══════════════════════════════════

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String title, String prop, double w) {
        TableColumn<S, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private Label label(String text, int size, String color, boolean bold) {
        Label l = new Label(text);
        l.setFont(bold ? Font.font("System", FontWeight.BOLD, size) : Font.font("System", size));
        l.setStyle("-fx-text-fill: " + color + ";");
        return l;
    }

    private Button styledBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand;");
        return b;
    }

    private Button dangerBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5; "
                + "-fx-font-weight: bold; -fx-cursor: hand;");
        return b;
    }

    private void formRow(GridPane grid, int row, String labelText, javafx.scene.Node field) {
        Label l = new Label(labelText);
        l.setStyle("-fx-text-fill: #e0e0e0;");
        grid.add(l, 0, row);
        grid.add(field, 1, row);
    }

    private void setOk(Label lbl, String msg) {
        lbl.setStyle("-fx-text-fill: #4ade80;");
        lbl.setText(msg);
    }

    private void setErr(Label lbl, String msg) {
        lbl.setStyle("-fx-text-fill: #e94560;");
        lbl.setText("Error: " + msg);
    }

    private java.util.Optional<ButtonType> confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirm");
        return a.showAndWait();
    }
}
