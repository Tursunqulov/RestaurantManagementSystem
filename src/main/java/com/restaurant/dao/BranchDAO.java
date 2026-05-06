package com.restaurant.dao;

import com.restaurant.model.Branch;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code branch} table.
 * Used by the Admin dashboard to manage restaurant branches.
 *
 * <p>All branches belong to restaurant_id = 1 (the single restaurant
 * in this system). A helper ensures the restaurant row always exists.</p>
 */
public class BranchDAO {

    private static final int DEFAULT_RESTAURANT_ID = 1;

    /** Returns only active branches. Used to populate ComboBoxes. */
    public List<Branch> findAll() throws SQLException {
        return findAll(false);
    }

    /**
     * Returns branches, optionally including inactive ones.
     * @param includeInactive true = show all; false = active only
     */
    public List<Branch> findAll(boolean includeInactive) throws SQLException {
        ensureRestaurantExists();
        List<Branch> list = new ArrayList<>();
        String sql = includeInactive
                ? "SELECT * FROM branch ORDER BY name"
                : "SELECT * FROM branch WHERE is_active = TRUE ORDER BY name";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Branch findById(int branchId) throws SQLException {
        String sql = "SELECT * FROM branch WHERE branch_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public int insert(Branch branch) throws SQLException {
        ensureRestaurantExists();
        String sql = """
                INSERT INTO branch (restaurant_id, name, street_address, is_active)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, DEFAULT_RESTAURANT_ID);
            ps.setString(2, branch.getName());
            ps.setString(3, branch.getLocation());
            ps.setBoolean(4, branch.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    branch.setBranchId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void update(Branch branch) throws SQLException {
        String sql = "UPDATE branch SET name = ?, street_address = ?, is_active = ? WHERE branch_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, branch.getName());
            ps.setString(2, branch.getLocation());
            ps.setBoolean(3, branch.isActive());
            ps.setInt(4, branch.getBranchId());
            ps.executeUpdate();
        }
    }

    public void setActive(int branchId, boolean active) throws SQLException {
        String sql = "UPDATE branch SET is_active = ? WHERE branch_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    /** Hard-deletes a branch. Use {@link #setActive} for a soft delete instead. */
    public void delete(int branchId) throws SQLException {
        String sql = "DELETE FROM branch WHERE branch_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.executeUpdate();
        }
    }

    private void ensureRestaurantExists() throws SQLException {
        String check = "SELECT COUNT(*) FROM restaurant WHERE restaurant_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(check)) {
            ps.setInt(1, DEFAULT_RESTAURANT_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }
        String insert = "INSERT INTO restaurant (restaurant_id, name) VALUES (?, 'Main Restaurant')";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(insert)) {
            ps.setInt(1, DEFAULT_RESTAURANT_ID);
            ps.executeUpdate();
        }
    }

    private Branch mapRow(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setBranchId(rs.getInt("branch_id"));
        b.setRestaurantId(rs.getInt("restaurant_id"));
        b.setName(rs.getString("name"));
        String loc = rs.getString("street_address");
        b.setLocation(loc != null ? loc : "");
        b.setActive(rs.getBoolean("is_active"));
        return b;
    }
}
