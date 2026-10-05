package com.vendingmachine.payment;

/**
 * One card attempt. The idempotency key belongs to the attempt, so it is held
 * here rather than passed in - which leaves PaymentMethod unchanged for cash.
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
            // a card is charged the exact price, so change is impossible by construction
            case PaymentResult.Approved approved -> new PaymentResult.Approved(0, approved.reference());
            case PaymentResult.Declined declined -> declined;
        };
    }
}
