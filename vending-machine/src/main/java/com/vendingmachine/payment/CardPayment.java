package com.vendingmachine.payment;

/** Card payment. Delegates to the gateway, and never returns change. */
public final class CardPayment implements PaymentMethod {

    private final Card card;
    private final PaymentGateway gateway;

    public CardPayment(Card card, PaymentGateway gateway) {
        this.card = card;
        this.gateway = gateway;
    }

    @Override
    public PaymentResult authorize(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        PaymentResult result = gateway.charge(card, amount);
        return switch (result) {
            // a card is charged the exact price, so change is structurally impossible
            case PaymentResult.Approved approved -> new PaymentResult.Approved(0, approved.reference());
            case PaymentResult.Declined declined -> declined;
        };
    }
}
