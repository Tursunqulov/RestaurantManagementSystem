package com.restaurant.model;

import com.restaurant.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a customer order placed by a waiter for a specific table.
 * An order contains one {@link Meal} per occupied seat.
 *
 * <p><b>Composition:</b> meals cannot exist without an order — deleting
 * an order cascades to its meals (enforced at both DB and object level).</p>
 */
public class Order {

    private int orderId;
    private int tableId;
    private Integer waiterId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private final List<Meal> meals = new ArrayList<>();

    public Order() { this.status = OrderStatus.RECEIVED; }

    public Order(int orderId, int tableId, Integer waiterId) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.waiterId = waiterId;
        this.status = OrderStatus.RECEIVED;
    }

    // ── Getters & Setters ───────────────────────────────────
    public int getOrderId()                         { return orderId; }
    public void setOrderId(int orderId)             { this.orderId = orderId; }

    public int getTableId()                         { return tableId; }
    public void setTableId(int tableId)             { this.tableId = tableId; }

    public Integer getWaiterId()                    { return waiterId; }
    public void setWaiterId(Integer waiterId)       { this.waiterId = waiterId; }

    public OrderStatus getStatus()                  { return status; }
    public void setStatus(OrderStatus status)       { this.status = status; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime ts)      { this.createdAt = ts; }

    public List<Meal> getMeals()                    { return Collections.unmodifiableList(meals); }
    public void addMeal(Meal meal)                  { meals.add(meal); }
    public void removeMeal(Meal meal)               { meals.remove(meal); }

    /**
     * Calculates the total price of this order by summing all meal items.
     * Demonstrates use of the Collections Framework (stream traversal).
     *
     * @return total price across every meal and meal item
     */
    public double calculateTotal() {
        return meals.stream()
                .flatMap(meal -> meal.getItems().stream())
                .mapToDouble(item -> item.getQuantity() * item.getMenuItemPrice())
                .sum();
    }

    @Override
    public String toString() {
        return "Order #" + orderId + " [" + status + "]";
    }
}
