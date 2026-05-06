package com.restaurant.model;

import com.restaurant.model.enums.PaymentType;

/**
 * Concrete Payment subclass for credit/debit card transactions.
 *
 * <p><b>Inheritance &amp; Polymorphism:</b> extends {@link Payment} and
 * provides a card-specific implementation of {@link #getPaymentDetail()}.
 * The {@link com.restaurant.dao.PaymentDAO} works through the {@code Payment}
 * reference — it never needs to know the concrete type until persistence time.</p>
 */
public class CreditCardPayment extends Payment {

    private String nameOnCard;
    private String cardLastFour;

    public CreditCardPayment(int paymentId, int billId, double amount,
                             String nameOnCard, String cardLastFour) {
        super(paymentId, billId, amount, PaymentType.CREDIT_CARD);
        this.nameOnCard  = nameOnCard;
        this.cardLastFour = cardLastFour;
    }

    public String getNameOnCard()               { return nameOnCard; }
    public void setNameOnCard(String name)       { this.nameOnCard = name; }
    public String getCardLastFour()              { return cardLastFour; }
    public void setCardLastFour(String last4)    { this.cardLastFour = last4; }

    @Override
    public String getPaymentDetail() {
        return nameOnCard + " — ****" + cardLastFour;
    }
}
