package com.restaurant.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Meal {
    private int mealId;
    private int orderId;
    private int seatId;
    private final List<MealItem> items = new ArrayList<>();

    public Meal(int mealId, int orderId, int seatId) {
        this.mealId = mealId;
        this.orderId = orderId;
        this.seatId = seatId;
    }

    public int getMealId()                  { return mealId; }
    public void setMealId(int mealId)       { this.mealId = mealId; }
    public int getOrderId()                 { return orderId; }
    public int getSeatId()                  { return seatId; }
    public List<MealItem> getItems()        { return Collections.unmodifiableList(items); }
    public void addItem(MealItem item)      { items.add(item); }

    @Override
    public String toString() { return "Meal #" + mealId + " — Seat #" + seatId; }
}