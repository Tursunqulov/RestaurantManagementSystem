package com.restaurant.dao;

import com.restaurant.model.Bill;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code bill} table.
 * Used by the Waiter dashboard when generating the final bill for an order.
 */
public class BillDAO {

    public Bill findByOrderId(int orderId) throws SQLException {
        String sql = "SELECT * FROM bill WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Creates a bill for an order.
     * Amount is computed from the order total; tax rate is 10%; tip defaults to 0.
     */
    public int insert(Bill bill) throws SQLException {
        String sql = """
                INSERT INTO bill (order_id, amount, tax, tip, is_paid)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bill.getOrderId());
            ps.setDouble(2, bill.getAmount());
            ps.setDouble(3, bill.getTax());
            ps.setDouble(4, bill.getTip());
            ps.setBoolean(5, bill.isPaid());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    bill.setBillId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void markPaid(int billId) throws SQLException {
        String sql = "UPDATE bill SET is_paid = TRUE WHERE bill_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        }
    }

    /** Returns all unpaid bills for a branch, with table_number populated. */
    public List<Bill> findUnpaidByBranch(int branchId) throws SQLException {
        List<Bill> list = new ArrayList<>();
        String sql = """
                SELECT b.*, rt.table_number
                FROM bill b
                JOIN `order` o  ON b.order_id  = o.order_id
                JOIN restaurant_table rt ON o.table_id = rt.table_id
                WHERE rt.branch_id = ? AND b.is_paid = FALSE
                ORDER BY b.created_at ASC
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bill b = mapRow(rs);
                    b.setTableNumber(rs.getString("table_number"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    public void updateTip(int billId, double tip) throws SQLException {
        String sql = "UPDATE bill SET tip = ? WHERE bill_id = ?";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, tip);
            ps.setInt(2, billId);
            ps.executeUpdate();
        }
    }

    private Bill mapRow(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getInt("bill_id"));
        b.setOrderId(rs.getInt("order_id"));
        b.setAmount(rs.getDouble("amount"));
        b.setTax(rs.getDouble("tax"));
        b.setTip(rs.getDouble("tip"));
        b.setPaid(rs.getBoolean("is_paid"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) b.setCreatedAt(ts.toLocalDateTime());
        return b;
    }
}
