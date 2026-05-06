package com.restaurant.model;

import com.restaurant.model.enums.PaymentType;

/**
 * Concrete Payment subclass for cheque payments.
 * Stores the bank name and cheque number for record-keeping.
 */
public class CheckPayment extends Payment {

    private String bankName;
    private String checkNumber;

    public CheckPayment(int paymentId, int billId, double amount,
                        String bankName, String checkNumber) {
        super(paymentId, billId, amount, PaymentType.CHECK);
        this.bankName    = bankName;
        this.checkNumber = checkNumber;
    }

    public String getBankName()                  { return bankName; }
    public void setBankName(String bankName)      { this.bankName = bankName; }
    public String getCheckNumber()               { return checkNumber; }
    public void setCheckNumber(String num)        { this.checkNumber = num; }

    @Override
    public String getPaymentDetail() {
        return "Check #" + checkNumber + " (" + bankName + ")";
    }
}
