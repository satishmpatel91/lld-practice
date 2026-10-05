package com.vendingmachine.payment;

/**
 * Card payment for one attempt. The idempotency key belongs to the attempt, so
 * it is held here rather than passed to authorize - which keeps PaymentMethod's
 * signature the same for cash.
 */
public final class CardPayment implements PaymentMethod {

    private final Card card;
    private final PaymentGateway gateway;
    private final String idempotencyKey;

    public CardPayment(Card card, PaymentGateway gateway, String idempotencyKey) {
        this.card = card;
        this.gateway = gateway;
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public PaymentResult authorize(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        PaymentResult result = gateway.charge(card, amount, idempotencyKey);
        return switch (result) {
            // a card is charged the exact price, so change is structurally impossible
            case PaymentResult.Approved approved -> new PaymentResult.Approved(0, approved.reference());
            case PaymentResult.Declined declined -> declined;
        };
    }
}
