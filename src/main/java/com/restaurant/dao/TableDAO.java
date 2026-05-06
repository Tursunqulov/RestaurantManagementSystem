package com.restaurant.dao;

import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableSeat;
import com.restaurant.model.enums.SeatType;
import com.restaurant.model.enums.TableStatus;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for {@code restaurant_table} and {@code table_seat}.
 * Auto-generates seat rows when a new table is inserted.
 */
public class TableDAO {

    // ══════════════════════ TABLE ════════════════════════════

    public List<RestaurantTable> findByBranch(int branchId) throws SQLException {
        List<RestaurantTable> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_table WHERE branch_id = ? ORDER BY table_number";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RestaurantTable t = mapTable(rs);
                    t.getSeats(); // already unmodifiable — load separately below
                    list.add(t);
                }
            }
        }
        // Eagerly load seats for each table
        for (RestaurantTable t : list) {
            loadSeats(t);
        }
        return list;
    }

    /** Returns only FREE tables for a branch (for walk-in availability). */
    public List<RestaurantTable> findFreeTables(int branchId) throws SQLException {
        List<RestaurantTable> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_table WHERE branch_id = ? AND status = 'FREE' ORDER BY table_number";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTable(rs));
            }
        }
        return list;
    }

    /** Finds free tables that can seat at least {@code minCapacity} guests. */
    public List<RestaurantTable> findAvailableTables(int branchId, int minCapacity) throws SQLException {
        List<RestaurantTable> list = new ArrayList<>();
        String sql = """
                SELECT * FROM restaurant_table
                WHERE branch_id = ? AND status = 'FREE' AND max_capacity >= ?
                ORDER BY max_capacity ASC
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, minCapacity);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTable(rs));
            }
        }
        return list;
    }

    public RestaurantTable findById(int tableId) throws SQLException {
        String sql = "SELECT * FROM restaurant_table WHERE table_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RestaurantTable t = mapTable(rs);
                    loadSeats(t);
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Inserts a table and auto-generates its seats.
     * Returns the generated table ID.
     */
    public int insert(RestaurantTable table) throws SQLException {
        String sql = """
                INSERT INTO restaurant_table (branch_id, table_number, max_capacity, location_identifier, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, table.getBranchId());
            ps.setString(2, table.getTableNumber());
            ps.setInt(3, table.getMaxCapacity());
            ps.setString(4, table.getLocationIdentifier());
            ps.setString(5, table.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    table.setTableId(id);
                    // Auto-generate seats
                    generateSeats(id, table.getMaxCapacity());
                    loadSeats(table);
                    return id;
                }
            }
        }
        return -1;
    }

    public void updateStatus(int tableId, TableStatus status) throws SQLException {
        String sql = "UPDATE restaurant_table SET status = ? WHERE table_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, tableId);
            ps.executeUpdate();
        }
    }

    /**
     * Returns tables with enough capacity that have no conflicting CONFIRMED/CHECKED_IN
     * reservations within ±2 hours of the requested time.
     */
    public List<RestaurantTable> findAvailableAtTime(int branchId, int minCapacity, LocalDateTime requestedTime) throws SQLException {
        List<RestaurantTable> list = new ArrayList<>();
        LocalDateTime from = requestedTime.minusHours(2);
        LocalDateTime to   = requestedTime.plusHours(2);
        String sql = """
                SELECT t.* FROM restaurant_table t
                WHERE t.branch_id = ? AND t.max_capacity >= ?
                  AND t.table_id NOT IN (
                      SELECT r.table_id FROM reservation r
                      WHERE r.status IN ('CONFIRMED','CHECKED_IN')
                        AND r.time_of_reservation BETWEEN ? AND ?
                  )
                ORDER BY t.max_capacity ASC
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, minCapacity);
            ps.setTimestamp(3, Timestamp.valueOf(from));
            ps.setTimestamp(4, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RestaurantTable t = mapTable(rs);
                    loadSeats(t);
                    list.add(t);
                }
            }
        }
        return list;
    }

    /** Deletes a table and all its seats. */
    public void delete(int tableId) throws SQLException {
        String delSeats = "DELETE FROM table_seat WHERE table_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(delSeats)) {
            ps.setInt(1, tableId);
            ps.executeUpdate();
        }
        String delTable = "DELETE FROM restaurant_table WHERE table_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(delTable)) {
            ps.setInt(1, tableId);
            ps.executeUpdate();
        }
    }

    // ══════════════════════ SEATS ════════════════════════════

    public List<TableSeat> findSeatsByTable(int tableId) throws SQLException {
        List<TableSeat> list = new ArrayList<>();
        String sql = "SELECT * FROM table_seat WHERE table_id = ? ORDER BY seat_number";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSeat(rs));
            }
        }
        return list;
    }

    /** Generates N seats for a newly created table. */
    private void generateSeats(int tableId, int count) throws SQLException {
        String sql = "INSERT INTO table_seat (table_id, seat_number, seat_type) VALUES (?, ?, 'REGULAR')";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                ps.setInt(1, tableId);
                ps.setInt(2, i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Loads seats into the table's in-memory list. */
    private void loadSeats(RestaurantTable table) throws SQLException {
        List<TableSeat> seats = findSeatsByTable(table.getTableId());
        for (TableSeat seat : seats) {
            table.addSeat(seat);
        }
    }

    // ══════════════════════ MAPPERS ══════════════════════════

    private RestaurantTable mapTable(ResultSet rs) throws SQLException {
        RestaurantTable t = new RestaurantTable();
        t.setTableId(rs.getInt("table_id"));
        t.setBranchId(rs.getInt("branch_id"));
        t.setTableNumber(rs.getString("table_number"));
        t.setMaxCapacity(rs.getInt("max_capacity"));
        t.setLocationIdentifier(rs.getString("location_identifier"));
        t.setStatus(TableStatus.valueOf(rs.getString("status")));
        return t;
    }

    private TableSeat mapSeat(ResultSet rs) throws SQLException {
        return new TableSeat(
                rs.getInt("seat_id"),
                rs.getInt("table_id"),
                rs.getInt("seat_number"),
                SeatType.valueOf(rs.getString("seat_type"))
        );
    }
}
