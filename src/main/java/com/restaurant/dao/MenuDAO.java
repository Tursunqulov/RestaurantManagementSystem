package com.restaurant.dao;

import com.restaurant.model.Menu;
import com.restaurant.model.MenuItem;
import com.restaurant.model.MenuSection;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO covering the three-level menu hierarchy:
 * {@link Menu} → {@link MenuSection} → {@link MenuItem}.
 *
 * <p><b>Single Responsibility:</b> all menu-related persistence in one place,
 * keeping the service layer free of SQL.</p>
 */
public class MenuDAO {

    // ══════════════════════ MENU ═════════════════════════════

    public Menu findMenuByBranch(int branchId) throws SQLException {
        String sql = "SELECT * FROM menu WHERE branch_id = ? LIMIT 1";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMenu(rs);
            }
        }
        return null;
    }

    /** Creates a menu for a branch (one per branch). Returns the generated ID. */
    public int insertMenu(Menu menu) throws SQLException {
        String sql = "INSERT INTO menu (branch_id, title, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, menu.getBranchId());
            ps.setString(2, menu.getTitle());
            ps.setString(3, menu.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    menu.setMenuId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Gets or creates the menu for a branch.
     * Ensures every branch always has exactly one menu.
     */
    public Menu getOrCreateMenu(int branchId) throws SQLException {
        Menu menu = findMenuByBranch(branchId);
        if (menu != null) return menu;
        menu = new Menu(0, branchId, "Main Menu", "Branch menu");
        insertMenu(menu);
        return menu;
    }

    // ══════════════════════ SECTION ══════════════════════════

    public List<MenuSection> findSectionsByMenu(int menuId) throws SQLException {
        List<MenuSection> list = new ArrayList<>();
        String sql = "SELECT * FROM menu_section WHERE menu_id = ? ORDER BY title";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, menuId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSection(rs));
            }
        }
        return list;
    }

    public void updateSection(MenuSection section) throws SQLException {
        String sql = "UPDATE menu_section SET title = ?, description = ? WHERE section_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, section.getTitle());
            ps.setString(2, section.getDescription());
            ps.setInt(3, section.getSectionId());
            ps.executeUpdate();
        }
    }

    /** Deletes a section and all its items. */
    public void deleteSection(int sectionId) throws SQLException {
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(
                "DELETE FROM menu_item WHERE section_id = ?")) {
            ps.setInt(1, sectionId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(
                "DELETE FROM menu_section WHERE section_id = ?")) {
            ps.setInt(1, sectionId);
            ps.executeUpdate();
        }
    }

    public int insertSection(MenuSection section) throws SQLException {
        String sql = "INSERT INTO menu_section (menu_id, title, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, section.getMenuId());
            ps.setString(2, section.getTitle());
            ps.setString(3, section.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    section.setSectionId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    // ══════════════════════ ITEM ═════════════════════════════

    public List<MenuItem> findItemsBySection(int sectionId) throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT * FROM menu_item WHERE section_id = ? ORDER BY title";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapItem(rs));
            }
        }
        return list;
    }

    /** Returns all menu items for a branch (across all sections). */
    public List<MenuItem> findAllItemsByBranch(int branchId) throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = """
                SELECT mi.* FROM menu_item mi
                JOIN menu_section ms ON mi.section_id = ms.section_id
                JOIN menu m ON ms.menu_id = m.menu_id
                WHERE m.branch_id = ? AND mi.is_available = TRUE
                ORDER BY ms.title, mi.title
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapItem(rs));
            }
        }
        return list;
    }

    public MenuItem findItemById(int itemId) throws SQLException {
        String sql = "SELECT * FROM menu_item WHERE item_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapItem(rs);
            }
        }
        return null;
    }

    public int insertItem(MenuItem item) throws SQLException {
        String sql = "INSERT INTO menu_item (section_id, title, description, price, is_available) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getSectionId());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getPrice());
            ps.setBoolean(5, item.isAvailable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    item.setItemId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void updateItem(MenuItem item) throws SQLException {
        String sql = "UPDATE menu_item SET title = ?, description = ?, price = ?, is_available = ? WHERE item_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, item.getTitle());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getPrice());
            ps.setBoolean(4, item.isAvailable());
            ps.setInt(5, item.getItemId());
            ps.executeUpdate();
        }
    }

    public void deleteItem(int itemId) throws SQLException {
        String sql = "DELETE FROM menu_item WHERE item_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }

    // ══════════════════════ ROW MAPPERS ══════════════════════

    private Menu mapMenu(ResultSet rs) throws SQLException {
        return new Menu(
                rs.getInt("menu_id"),
                rs.getInt("branch_id"),
                rs.getString("title"),
                rs.getString("description")
        );
    }

    private MenuSection mapSection(ResultSet rs) throws SQLException {
        return new MenuSection(
                rs.getInt("section_id"),
                rs.getInt("menu_id"),
                rs.getString("title"),
                rs.getString("description")
        );
    }

    private MenuItem mapItem(ResultSet rs) throws SQLException {
        MenuItem item = new MenuItem(
                rs.getInt("item_id"),
                rs.getInt("section_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDouble("price")
        );
        item.setAvailable(rs.getBoolean("is_available"));
        return item;
    }
}
