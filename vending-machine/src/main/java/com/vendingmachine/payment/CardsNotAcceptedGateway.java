package com.vendingmachine.payment;

/**
 * Null object for a machine with no card reader. Declining is the honest
 * answer: "no card reader" is a known outcome, not a system failure, so it
 * travels as a result like every other decline.
 */
public final class CardsNotAcceptedGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Card card, int amount, String idempotencyKey) {
        return new PaymentResult.Declined("Card payments are not available.");
    }
}
