package com.restaurant.controller;

import com.restaurant.model.User;
import com.restaurant.service.ReservationNotificationService;
import javafx.scene.control.Alert;

/**
 * Routes a logged-in user to the correct dashboard based on their role.
 * Called by {@link com.restaurant.Main} immediately after successful login.
 *
 * <p><b>Single Responsibility:</b> this class only decides WHICH dashboard
 * to open — it does not contain any dashboard UI code itself.</p>
 */
public final class DashboardRouter {

    private DashboardRouter() {}

    public static void open(User user, ReservationNotificationService notificationService) {
        switch (user.getRole()) {
            case MANAGER      -> ManagerDashboard.open(user);
            case RECEPTIONIST -> ReceptionistDashboard.open(user, notificationService);
            case WAITER       -> WaiterDashboard.open(user);
            case CASHIER -> CashierDashboard.open(user);
            case CHEF    -> ChefDashboard.open(user);
            default -> {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Unknown role: " + user.getRole());
                alert.showAndWait();
            }
        }
    }
}
