package com.vendingmachine.payment;

/**
 * Null object for a machine with no card reader. "No card reader" is a known
 * answer, not a failure, so it travels as a decline like any other.
 */
public final class CardsNotAcceptedGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Card card, int amount, String idempotencyKey) {
        return new PaymentResult.Declined("Card payments are not available.");
    }
}
