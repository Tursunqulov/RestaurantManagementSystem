package com.restaurant.model;

import java.util.ArrayList;
import java.util.List;

public class Menu {
    private int menuId;
    private int branchId;
    private String title;
    private String description;
    private List<MenuSection> sections = new ArrayList<>();

    public Menu(int menuId, int branchId, String title, String description) {
        this.menuId = menuId;
        this.branchId = branchId;
        this.title = title;
        this.description = description;
    }

    public int getMenuId()                             { return menuId; }
    public void setMenuId(int menuId)                  { this.menuId = menuId; }
    public int getBranchId()                           { return branchId; }
    public void setBranchId(int branchId)              { this.branchId = branchId; }
    public String getTitle()                           { return title; }
    public void setTitle(String title)                 { this.title = title; }
    public String getDescription()                     { return description; }
    public void setDescription(String description)     { this.description = description; }
    public List<MenuSection> getSections()             { return sections; }
    public void setSections(List<MenuSection> sections){ this.sections = sections; }
    public void addSection(MenuSection section)        { this.sections.add(section); }
}