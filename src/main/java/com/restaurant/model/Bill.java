package com.restaurant.model;

import java.time.LocalDateTime;

public class Bill {
    private int billId;
    private int orderId;
    private double amount;
    private double tax;
    private double tip;
    private boolean isPaid;
    private LocalDateTime createdAt;
    private transient String tableNumber;

    public Bill() {}

    public Bill(int billId, int orderId, double amount, double tax, double tip) {
        this.billId = billId;
        this.orderId = orderId;
        this.amount = amount;
        this.tax = tax;
        this.tip = tip;
        this.isPaid = false;
    }

    public int getBillId()                           { return billId; }
    public void setBillId(int billId)                { this.billId = billId; }
    public int getOrderId()                          { return orderId; }
    public void setOrderId(int orderId)              { this.orderId = orderId; }
    public double getAmount()                        { return amount; }
    public void setAmount(double amount)             { this.amount = amount; }
    public double getTax()                           { return tax; }
    public void setTax(double tax)                   { this.tax = tax; }
    public double getTip()                           { return tip; }
    public void setTip(double tip)                   { this.tip = tip; }
    public boolean isPaid()                          { return isPaid; }
    public void setPaid(boolean isPaid)              { this.isPaid = isPaid; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime ts)       { this.createdAt = ts; }

    public String getTableNumber()                    { return tableNumber; }
    public void setTableNumber(String tableNumber)    { this.tableNumber = tableNumber; }

    public double getTotal() { return amount + tax + tip; }
}