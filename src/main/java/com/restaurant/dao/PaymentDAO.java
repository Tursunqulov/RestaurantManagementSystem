package com.restaurant.dao;

import com.restaurant.model.*;
import com.restaurant.model.enums.PaymentType;
import com.restaurant.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code payment} table.
 *
 * <p><b>Polymorphism in action:</b> the {@link #insert} method accepts any
 * {@link Payment} subclass and persists the type-specific fields using
 * {@code instanceof} pattern matching (Java 21 feature). Reading back
 * from the DB uses the factory method {@link #mapRow} to reconstruct
 * the correct subclass.</p>
 */
public class PaymentDAO {

    public List<Payment> findByBillId(int billId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payment WHERE bill_id = ? ORDER BY created_at";
        try (PreparedStatement ps = DatabaseUtil.getConnection().prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Inserts a payment record. Uses polymorphic dispatch to extract
     * subclass-specific fields (card details, check info, or cash amount).
     */
    public int insert(Payment payment) throws SQLException {
        String sql = """
                INSERT INTO payment
                    (bill_id, amount, payment_type,
                     name_on_card, card_last_four,
                     bank_name, check_number,
                     cash_tendered)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DatabaseUtil.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getBillId());
            ps.setDouble(2, payment.getAmount());
            ps.setString(3, payment.getPaymentType().name());

            // ── Polymorphic field extraction (Java 21 pattern matching) ──
            if (payment instanceof CreditCardPayment cc) {
                ps.setString(4, cc.getNameOnCard());
                ps.setString(5, cc.getCardLastFour());
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.DECIMAL);
            } else if (payment instanceof CheckPayment chk) {
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.VARCHAR);
                ps.setString(6, chk.getBankName());
                ps.setString(7, chk.getCheckNumber());
                ps.setNull(8, Types.DECIMAL);
            } else if (payment instanceof CashPayment cash) {
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.VARCHAR);
                ps.setDouble(8, cash.getCashTendered());
            } else {
                // Fallback — should not happen if hierarchy is correct
                for (int i = 4; i <= 8; i++) ps.setNull(i, Types.VARCHAR);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    payment.setPaymentId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Factory method — reconstructs the correct {@link Payment} subclass
     * based on the {@code payment_type} column.
     *
     * <p>Demonstrates the <b>Open/Closed Principle</b>: adding a new
     * payment type requires only a new {@code case} here and a new
     * subclass — no changes to existing subclasses.</p>
     */
    private Payment mapRow(ResultSet rs) throws SQLException {
        PaymentType type = PaymentType.valueOf(rs.getString("payment_type"));
        int id      = rs.getInt("payment_id");
        int billId  = rs.getInt("bill_id");
        double amt  = rs.getDouble("amount");

        Payment payment = switch (type) {
            case CREDIT_CARD -> {
                CreditCardPayment cc = new CreditCardPayment(id, billId, amt,
                        rs.getString("name_on_card"),
                        rs.getString("card_last_four"));
                yield cc;
            }
            case CHECK -> {
                CheckPayment chk = new CheckPayment(id, billId, amt,
                        rs.getString("bank_name"),
                        rs.getString("check_number"));
                yield chk;
            }
            case CASH -> {
                double tendered = rs.getDouble("cash_tendered");
                CashPayment cash = new CashPayment(id, billId, amt, tendered);
                yield cash;
            }
        };

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) payment.setCreatedAt(ts.toLocalDateTime());
        return payment;
    }
}
