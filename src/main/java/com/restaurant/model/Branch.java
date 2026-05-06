package com.restaurant.model;

import java.util.ArrayList;
import java.util.List;

public class Branch {
    private int branchId;
    private int restaurantId;
    private String name;
    private String location;
    private boolean active;
    private List<RestaurantTable> tables = new ArrayList<>();

    public Branch() { this.active = true; }

    public Branch(int branchId, int restaurantId, String name, String location) {
        this.branchId = branchId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.location = location;
        this.active = true;
    }

    public int getBranchId()                          { return branchId; }
    public void setBranchId(int branchId)             { this.branchId = branchId; }
    public int getRestaurantId()                      { return restaurantId; }
    public void setRestaurantId(int restaurantId)     { this.restaurantId = restaurantId; }
    public String getName()                           { return name; }
    public void setName(String name)                  { this.name = name; }
    public String getLocation()                       { return location; }
    public void setLocation(String location)          { this.location = location; }
    public boolean isActive()                         { return active; }
    public void setActive(boolean active)             { this.active = active; }
    public List<RestaurantTable> getTables()          { return tables; }
    public void setTables(List<RestaurantTable> t)    { this.tables = t; }

    @Override
    public String toString() { return name + " (" + location + ")"; }
}