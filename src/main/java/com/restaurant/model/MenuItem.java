package com.restaurant.model;

public class MenuItem {
    private int itemId;
    private int sectionId;
    private String title;
    private String description;
    private double price;
    private boolean available;

    public MenuItem(int itemId, int sectionId, String title, String description, double price) {
        this.itemId = itemId;
        this.sectionId = sectionId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.available = true;
    }

    public int getItemId()                           { return itemId; }
    public void setItemId(int itemId)                { this.itemId = itemId; }
    public int getSectionId()                        { return sectionId; }
    public void setSectionId(int sectionId)          { this.sectionId = sectionId; }
    public String getTitle()                         { return title; }
    public void setTitle(String title)               { this.title = title; }
    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }
    public double getPrice()                         { return price; }
    public void setPrice(double price)               { this.price = price; }
    public boolean isAvailable()                     { return available; }
    public void setAvailable(boolean available)      { this.available = available; }

    @Override
    public String toString() { return title + " — $" + String.format("%.2f", price); }
}