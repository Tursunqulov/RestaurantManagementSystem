package com.restaurant.model;

/**
 * One line-item within a Meal — links a MenuIitem to a seat's meal
 * and records how many were ordered.
 * menuItemTitle and menuItemPrice are transient: loaded by the DAO
 * via a JOIN so the UI can display prices without a second query.
 */
public class MealItem {
    private int mealItemId;
    private int mealId;
    private int menuItemId;
    private int quantity;
    private String menuItemTitle;
    private double menuItemPrice;

    public MealItem(int mealItemId, int mealId, int menuItemId, int quantity) {
        this.mealItemId = mealItemId;
        this.mealId = mealId;
        this.menuItemId = menuItemId;
        this.quantity = quantity;
    }

    public int getMealItemId()                         { return mealItemId; }
    public void setMealItemId(int mealItemId)          { this.mealItemId = mealItemId; }
    public int getMealId()                             { return mealId; }
    public int getMenuItemId()                         { return menuItemId; }
    public int getQuantity()                           { return quantity; }
    public void setQuantity(int quantity)              { this.quantity = quantity; }
    public String getMenuItemTitle()                   { return menuItemTitle; }
    public void setMenuItemTitle(String title)         { this.menuItemTitle = title; }
    public double getMenuItemPrice()                   { return menuItemPrice; }
    public void setMenuItemPrice(double price)         { this.menuItemPrice = price; }

    public double getLineTotal() { return quantity * menuItemPrice; }

    @Override
    public String toString() {
        return quantity + "x " + menuItemTitle + " ($" + String.format("%.2f", menuItemPrice) + ")";
    }
}