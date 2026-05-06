package com.restaurant.model;

import com.restaurant.model.enums.PaymentType;

import java.time.LocalDateTime;

/**
 * <b>Abstract base class</b> for all payment methods.
 *
 * <h3>OOP Principles Demonstrated:</h3>
 * <ul>
 *   <li><b>Abstraction</b> — this class cannot be instantiated directly;
 *       concrete subclasses ({@link CreditCardPayment}, {@link CheckPayment},
 *       {@link CashPayment}) provide the implementation.</li>
 *   <li><b>Polymorphism</b> — the {@link #getPaymentDetail()} method is
 *       overridden by each subclass to return type-specific information,
 *       while callers work with the {@code Payment} reference.</li>
 *   <li><b>Inheritance</b> — common fields (amount, billId, createdAt)
 *       are defined once here and inherited by all subclasses.</li>
 *   <li><b>Encapsulation</b> — all fields are private with controlled access.</li>
 * </ul>
 *
 * <h3>SOLID — Open/Closed Principle (O):</h3>
 * <p>New payment methods (e.g. mobile wallet) can be added by creating a
 * new subclass without modifying any existing code.</p>
 */
public abstract class Payment {

    private int paymentId;
    private int billId;
    private double amount;
    private PaymentType paymentType;
    private LocalDateTime createdAt;

    // ── Protected constructor (only subclasses may call) ────
    protected Payment() {}

    protected Payment(int paymentId, int billId, double amount, PaymentType paymentType) {
        this.paymentId = paymentId;
        this.billId = billId;
        this.amount = amount;
        this.paymentType = paymentType;
    }

    // ── Getters & Setters ───────────────────────────────────
    public int getPaymentId()                           { return paymentId; }
    public void setPaymentId(int paymentId)             { this.paymentId = paymentId; }

    public int getBillId()                              { return billId; }
    public void setBillId(int billId)                   { this.billId = billId; }

    public double getAmount()                           { return amount; }
    public void setAmount(double amount)                { this.amount = amount; }

    public PaymentType getPaymentType()                 { return paymentType; }
    public void setPaymentType(PaymentType type)        { this.paymentType = type; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime ts)          { this.createdAt = ts; }

    // ── Abstract contract ───────────────────────────────────

    /**
     * Returns a human-readable summary of the payment-specific details.
     * Each subclass provides its own implementation (polymorphism).
     *
     * @return detail string, e.g. "Visa ****1234" or "Cash tendered: 100 000"
     */
    public abstract String getPaymentDetail();

    @Override
    public String toString() {
        return paymentType + " — " + String.format("%.2f", amount)
                + " | " + getPaymentDetail();
    }
}
