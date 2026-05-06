package com.restaurant.dao;

import com.restaurant.model.Reservation;
import com.restaurant.model.enums.ReservationStatus;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code reservation} table.
 * Supports creation, cancellation, and time-based queries
 * used by both the Receptionist dashboard and the notification daemon.
 */
public class ReservationDAO {

    public List<Reservation> findByBranch(int branchId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = """
                SELECT r.* FROM reservation r
                JOIN restaurant_table t ON r.table_id = t.table_id
                WHERE t.branch_id = ? AND r.status NOT IN ('CANCELED','ABANDONED')
                ORDER BY r.time_of_reservation DESC
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Reservation findById(int reservationId) throws SQLException {
        String sql = "SELECT * FROM reservation WHERE reservation_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Finds upcoming confirmed reservations within a time window.
     * Used by the background notification daemon to detect approaching reservations.
     *
     * @param from start of window (inclusive)
     * @param to   end of window (inclusive)
     * @return list of confirmed reservations in the window
     */
    public List<Reservation> findUpcoming(LocalDateTime from, LocalDateTime to) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = """
                SELECT * FROM reservation
                WHERE status = 'CONFIRMED'
                  AND time_of_reservation BETWEEN ? AND ?
                ORDER BY time_of_reservation ASC
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int insert(Reservation reservation) throws SQLException {
        String sql = """
                INSERT INTO reservation
                    (table_id, customer_id, reserved_by_user_id,
                     time_of_reservation, people_count, status, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reservation.getTableId());
            setNullableInt(ps, 2, reservation.getCustomerId());
            setNullableInt(ps, 3, reservation.getReservedByUserId());
            ps.setTimestamp(4, Timestamp.valueOf(reservation.getTimeOfReservation()));
            ps.setInt(5, reservation.getPeopleCount());
            ps.setString(6, reservation.getStatus().name());
            ps.setString(7, reservation.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    reservation.setReservationId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void updateStatus(int reservationId, ReservationStatus status) throws SQLException {
        String sql = "UPDATE reservation SET status = ? WHERE reservation_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }
    }

    public void cancel(int reservationId) throws SQLException {
        updateStatus(reservationId, ReservationStatus.CANCELED);
    }

    public void checkIn(int reservationId) throws SQLException {
        String sql = "UPDATE reservation SET status = 'CHECKED_IN', checkin_time = NOW() WHERE reservation_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    public void update(Reservation reservation) throws SQLException {
        String sql = """
                UPDATE reservation
                SET table_id = ?, time_of_reservation = ?, people_count = ?, notes = ?, status = ?
                WHERE reservation_id = ?
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, reservation.getTableId());
            ps.setTimestamp(2, Timestamp.valueOf(reservation.getTimeOfReservation()));
            ps.setInt(3, reservation.getPeopleCount());
            ps.setString(4, reservation.getNotes());
            ps.setString(5, reservation.getStatus().name());
            ps.setInt(6, reservation.getReservationId());
            ps.executeUpdate();
        }
    }

    /** Searches reservations by customer name or phone for a given branch. */
    public List<Reservation> searchByCustomer(String query, int branchId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = """
                SELECT r.* FROM reservation r
                JOIN restaurant_table t ON r.table_id = t.table_id
                LEFT JOIN customer c ON r.customer_id = c.customer_id
                WHERE t.branch_id = ?
                  AND r.status NOT IN ('CANCELED','ABANDONED')
                  AND (c.full_name LIKE ? OR c.phone LIKE ?)
                ORDER BY r.time_of_reservation DESC
                """;
        String pattern = "%" + query + "%";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Helpers ─────────────────────────────────────────────

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setTableId(rs.getInt("table_id"));
        int cid = rs.getInt("customer_id");
        r.setCustomerId(rs.wasNull() ? null : cid);
        int uid = rs.getInt("reserved_by_user_id");
        r.setReservedByUserId(rs.wasNull() ? null : uid);
        r.setTimeOfReservation(rs.getTimestamp("time_of_reservation").toLocalDateTime());
        r.setPeopleCount(rs.getInt("people_count"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setNotes(rs.getString("notes"));
        Timestamp ci = rs.getTimestamp("checkin_time");
        if (ci != null) r.setCheckinTime(ci.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) r.setCreatedAt(ca.toLocalDateTime());
        return r;
    }
}
