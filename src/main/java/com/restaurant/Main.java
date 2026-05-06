package com.restaurant;

import com.restaurant.service.ReservationNotificationService;
import com.restaurant.util.DatabaseUtil;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Application entry point.
 *
 * <p>Extends {@link Application} as required by JavaFX.
 * The static {@link #main} calls {@link #launch} which bootstraps
 * the JavaFX toolkit and calls {@link #start(Stage)} on the FX thread.</p>
 */
public class Main extends Application {

    private static final ReservationNotificationService notificationService =
            new ReservationNotificationService();

    private static Main instance;
    private Stage primaryStage;
    private TextField userField;
    private PasswordField passField;
    private Label statusLabel;

    /** Re-shows the login window after a dashboard logout. */
    public static void showLogin() {
        instance.userField.clear();
        instance.passField.clear();
        instance.statusLabel.setText("");
        instance.primaryStage.show();
    }

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;

        primaryStage.setTitle("Restaurant Management System — Login");
        primaryStage.setResizable(false);

        // ── Root layout ──────────────────────────────────────
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #1a1a2e;");

        // ── Title ────────────────────────────────────────────
        Label title = new Label("Restaurant Management System");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #e94560;");

        Label subtitle = new Label("Please sign in to continue");
        subtitle.setStyle("-fx-text-fill: #a8a8b3;");

        // ── Form grid ────────────────────────────────────────
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-text-fill: #e0e0e0;");
        userField = new TextField();
        userField.setPromptText("Enter username");
        userField.setPrefWidth(260);

        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-text-fill: #e0e0e0;");
        passField = new PasswordField();
        passField.setPromptText("Enter password");

        form.add(userLabel, 0, 0);
        form.add(userField, 1, 0);
        form.add(passLabel, 0, 1);
        form.add(passField, 1, 1);

        // ── Status label ─────────────────────────────────────
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #e94560;");

        // ── Login button ─────────────────────────────────────
        Button loginBtn = new Button("Login");
        loginBtn.setPrefWidth(120);
        loginBtn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-cursor: hand;");

        loginBtn.setOnAction(e -> handleLogin(
                userField.getText().trim(),
                passField.getText(),
                statusLabel
        ));

        // Allow Enter key to trigger login
        passField.setOnAction(e -> loginBtn.fire());

        root.getChildren().addAll(title, subtitle, form, loginBtn, statusLabel);

        Scene scene = new Scene(root, 480, 340);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the background notification daemon
        notificationService.start();
    }

    private void handleLogin(String username, String password, Label status) {
        if (username.isEmpty() || password.isEmpty()) {
            status.setText("Please enter both username and password.");
            return;
        }

        // Show a loading message so the user knows it's working
        status.setStyle("-fx-text-fill: #a8a8b3;");
        status.setText("Authenticating...");

        // 1. Move the Database call to a Background Thread so Wayland doesn't freeze
        new Thread(() -> {
            try {
                com.restaurant.dao.UserDAO dao = new com.restaurant.dao.UserDAO();
                java.util.Optional<com.restaurant.model.User> opt = dao.findByUsername(username);

                // 2. Once DB is done, jump BACK to the JavaFX UI Thread to change scenes
                javafx.application.Platform.runLater(() -> {
                    if (opt.isEmpty() || !opt.get().checkPassword(password)) {
                        status.setStyle("-fx-text-fill: #e94560;");
                        status.setText("Invalid username or password.");
                        return;
                    }

                    com.restaurant.model.User user = opt.get();
                    if (!user.isActive()) {
                        status.setStyle("-fx-text-fill: #e94560;");
                        status.setText("Account is deactivated. Contact admin.");
                        return;
                    }

                    status.setText("");
                    primaryStage.hide();
                    com.restaurant.controller.DashboardRouter.open(user, notificationService);
                });

            } catch (java.sql.SQLException ex) {
                // Handle DB errors on the UI thread safely
                javafx.application.Platform.runLater(() -> {
                    status.setStyle("-fx-text-fill: #e94560;");
                    status.setText("Database error: " + ex.getMessage());
                    ex.printStackTrace();
                });
            }
        }).start(); // Start the background thread
    }

    @Override
    public void stop() {
        notificationService.stop();
        DatabaseUtil.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}