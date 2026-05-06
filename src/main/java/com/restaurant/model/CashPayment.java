package com.restaurant.model;

import com.restaurant.model.enums.PaymentType;

/**
 * Concrete Payment subclass for cash transactions.
 * Tracks cash tendered so the waiter can compute change.
 */
public class CashPayment extends Payment {

    private double cashTendered;

    public CashPayment(int paymentId, int billId, double amount, double cashTendered) {
        super(paymentId, billId, amount, PaymentType.CASH);
        this.cashTendered = cashTendered;
    }

    public double getCashTendered()              { return cashTendered; }
    public void setCashTendered(double tendered) { this.cashTendered = tendered; }

    public double getChange() { return cashTendered - getAmount(); }

    @Override
    public String getPaymentDetail() {
        return String.format("Cash tendered: %.2f | Change: %.2f", cashTendered, getChange());
    }
}
