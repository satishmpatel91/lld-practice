package com.vendingmachine.payment;

/**
 * The boundary to somebody else's computer, declared by the domain that needs
 * it. Tests use a scripted stand-in, because no real gateway can be told to
 * decline on demand.
 */
public interface PaymentGateway {

    /**
     * @param idempotencyKey identifies the attempt, not the card: the same key
     *                       presented twice must move money once
     * @throws PaymentGatewayException when the outcome is unknown
     */
    PaymentResult charge(Card card, int amount, String idempotencyKey);
}
