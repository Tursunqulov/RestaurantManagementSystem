package com.restaurant.dao;

import com.restaurant.model.User;
import com.restaurant.model.enums.UserRole;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code users} table.
 *
 * <p><b>Single Responsibility (S):</b> this class handles <em>only</em>
 * persistence logic for {@link User} — no business rules, no UI.</p>
 */
public class UserDAO {

    // ── Authentication ──────────────────────────────────────

    /**
     * Looks up a user by username, returning an {@link Optional} to
     * force callers to handle the "not found" case explicitly.
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ── CRUD ────────────────────────────────────────────────

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /** Returns all users, optionally filtered by branch. */
    public List<User> findAll(Integer branchId) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = (branchId != null)
                ? "SELECT * FROM users WHERE branch_id = ? ORDER BY username"
                : "SELECT * FROM users ORDER BY username";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            if (branchId != null) ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Returns all employees for a given branch. */
    public List<User> findEmployeesByBranch(int branchId) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE branch_id = ? ORDER BY role, username";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Inserts a new user and returns the generated ID.
     * Used by the Admin dashboard to create Waiters / Receptionists.
     */
    public int insert(User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, password, full_name, email, phone, role, branch_id, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            // Password stored via a dedicated setter — no getter exists on User
            ps.setString(2, getPasswordForInsert(user));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole().name());
            if (user.getBranchId() != null) {
                ps.setInt(7, user.getBranchId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setBoolean(8, user.isActive());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    user.setUserId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void update(User user) throws SQLException {
        String sql = """
                UPDATE users SET full_name = ?, email = ?, phone = ?,
                       role = ?, branch_id = ?, is_active = ?
                WHERE user_id = ?
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getRole().name());
            if (user.getBranchId() != null) {
                ps.setInt(5, user.getBranchId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setBoolean(6, user.isActive());
            ps.setInt(7, user.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void changePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ── Internal helpers ────────────────────────────────────

    /**
     * Maps a ResultSet row to a User object.
     * Password IS loaded so that {@link User#checkPassword} works during login.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));      // write-only on User
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(UserRole.valueOf(rs.getString("role")));
        int bid = rs.getInt("branch_id");
        u.setBranchId(rs.wasNull() ? null : bid);
        u.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }

    /**
     * Accesses the password for INSERT via the persistence-only accessor.
     */
    private String getPasswordForInsert(User user) {
        return user.getPasswordForStorage();
    }
}
