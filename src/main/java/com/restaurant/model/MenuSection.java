package com.restaurant.model;

import java.util.ArrayList;
import java.util.List;

public class MenuSection {
    private int sectionId;
    private int menuId;
    private String title;
    private String description;
    private List<MenuItem> items = new ArrayList<>();

    public MenuSection(int sectionId, int menuId, String title, String description) {
        this.sectionId = sectionId;
        this.menuId = menuId;
        this.title = title;
        this.description = description;
    }

    public int getSectionId()                        { return sectionId; }
    public void setSectionId(int sectionId)          { this.sectionId = sectionId; }
    public int getMenuId()                           { return menuId; }
    public void setMenuId(int menuId)                { this.menuId = menuId; }
    public String getTitle()                         { return title; }
    public void setTitle(String title)               { this.title = title; }
    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }
    public List<MenuItem> getItems()                 { return items; }
    public void setItems(List<MenuItem> items)       { this.items = items; }
    public void addItem(MenuItem item)               { this.items.add(item); }

    @Override
    public String toString() { return title; }
}