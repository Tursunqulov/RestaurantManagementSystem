package com.restaurant.dao;

import com.restaurant.model.Meal;
import com.restaurant.model.MealItem;
import com.restaurant.model.Order;
import com.restaurant.model.enums.OrderStatus;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO covering the three-level order hierarchy:
 * {@link Order} → {@link Meal} → {@link MealItem}.
 *
 * <p>Meals and meal items are eagerly loaded when an order is fetched,
 * using the {@link java.util.ArrayList} collections on each model.</p>
 */
public class OrderDAO {

    // ══════════════════════ ORDER ════════════════════════════

    public Order findById(int orderId) throws SQLException {
        String sql = "SELECT * FROM `order` WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    loadMeals(order);
                    return order;
                }
            }
        }
        return null;
    }

    /** Finds active (non-canceled, non-complete) orders for a table. */
    public Order findActiveOrderForTable(int tableId) throws SQLException {
        String sql = "SELECT * FROM `order` WHERE table_id = ? AND status IN ('RECEIVED','PREPARING') ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    loadMeals(order);
                    return order;
                }
            }
        }
        return null;
    }

    /** Returns all orders for a given branch (via table → branch join). */
    public List<Order> findByBranch(int branchId) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = """
                SELECT o.* FROM `order` o
                JOIN restaurant_table t ON o.table_id = t.table_id
                WHERE t.branch_id = ?
                ORDER BY o.created_at DESC
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapOrder(rs));
            }
        }
        // Eager-load meals for each order
        for (Order order : list) {
            loadMeals(order);
        }
        return list;
    }

    public int insert(Order order) throws SQLException {
        String sql = "INSERT INTO `order` (table_id, waiter_id, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getTableId());
            if (order.getWaiterId() != null) {
                ps.setInt(2, order.getWaiterId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, order.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    order.setOrderId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void updateStatus(int orderId, OrderStatus status) throws SQLException {
        String sql = "UPDATE `order` SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    // ══════════════════════ MEAL ═════════════════════════════

    public int insertMeal(Meal meal) throws SQLException {
        String sql = "INSERT INTO meal (order_id, seat_id) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, meal.getOrderId());
            ps.setInt(2, meal.getSeatId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    meal.setMealId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /** Finds or creates a meal for a specific seat within an order. */
    public Meal getOrCreateMeal(int orderId, int seatId) throws SQLException {
        String sql = "SELECT * FROM meal WHERE order_id = ? AND seat_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, seatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Meal meal = mapMeal(rs);
                    loadMealItems(meal);
                    return meal;
                }
            }
        }
        // Create new meal
        Meal meal = new Meal(0, orderId, seatId);
        insertMeal(meal);
        return meal;
    }

    // ══════════════════════ MEAL ITEM ════════════════════════

    public int insertMealItem(MealItem item) throws SQLException {
        String sql = "INSERT INTO meal_item (meal_id, menu_item_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getMealId());
            ps.setInt(2, item.getMenuItemId());
            ps.setInt(3, item.getQuantity());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    item.setMealItemId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void deleteMealItem(int mealItemId) throws SQLException {
        String sql = "DELETE FROM meal_item WHERE meal_item_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, mealItemId);
            ps.executeUpdate();
        }
    }

    public void deleteMeal(int mealId) throws SQLException {
        String delItems = "DELETE FROM meal_item WHERE meal_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(delItems)) {
            ps.setInt(1, mealId);
            ps.executeUpdate();
        }
        String delMeal = "DELETE FROM meal WHERE meal_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(delMeal)) {
            ps.setInt(1, mealId);
            ps.executeUpdate();
        }
    }

    public void updateMealItemQuantity(int mealItemId, int quantity) throws SQLException {
        String sql = "UPDATE meal_item SET quantity = ? WHERE meal_item_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, mealItemId);
            ps.executeUpdate();
        }
    }

    /** Returns all RECEIVED/PREPARING orders for a branch. */
    public List<Order> findActiveByBranch(int branchId) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = """
                SELECT o.* FROM `order` o
                JOIN restaurant_table t ON o.table_id = t.table_id
                WHERE t.branch_id = ? AND o.status IN ('RECEIVED','PREPARING')
                ORDER BY o.created_at ASC
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapOrder(rs));
            }
        }
        for (Order order : list) loadMeals(order);
        return list;
    }

    // ══════════════════════ EAGER LOADERS ════════════════════

    private void loadMeals(Order order) throws SQLException {
        String sql = "SELECT * FROM meal WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, order.getOrderId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Meal meal = mapMeal(rs);
                    loadMealItems(meal);
                    order.addMeal(meal);
                }
            }
        }
    }

    private void loadMealItems(Meal meal) throws SQLException {
        String sql = """
                SELECT mi.*, m.title AS menu_title, m.price AS menu_price
                FROM meal_item mi
                JOIN menu_item m ON mi.menu_item_id = m.item_id
                WHERE mi.meal_id = ?
                ORDER BY mi.meal_item_id
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, meal.getMealId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MealItem item = mapMealItem(rs);
                    item.setMenuItemTitle(rs.getString("menu_title"));
                    item.setMenuItemPrice(rs.getDouble("menu_price"));
                    meal.addItem(item);
                }
            }
        }
    }

    // ══════════════════════ MAPPERS ══════════════════════════

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setTableId(rs.getInt("table_id"));
        int wid = rs.getInt("waiter_id");
        o.setWaiterId(rs.wasNull() ? null : wid);
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) o.setCreatedAt(ts.toLocalDateTime());
        return o;
    }

    private Meal mapMeal(ResultSet rs) throws SQLException {
        return new Meal(
                rs.getInt("meal_id"),
                rs.getInt("order_id"),
                rs.getInt("seat_id")
        );
    }

    private MealItem mapMealItem(ResultSet rs) throws SQLException {
        return new MealItem(
                rs.getInt("meal_item_id"),
                rs.getInt("meal_id"),
                rs.getInt("menu_item_id"),
                rs.getInt("quantity")
        );
    }
}
