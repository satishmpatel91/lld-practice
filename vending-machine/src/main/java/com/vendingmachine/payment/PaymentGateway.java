package com.vendingmachine.payment;

/**
 * The external world, declared by the domain that needs it (dependency
 * inversion). A real implementation wraps a bank SDK; tests use a scripted
 * stand-in, since no real gateway can be told to decline on demand.
 */
public interface PaymentGateway {

    /**
     * @param idempotencyKey identifies the ATTEMPT, not the card. The same key
     *                       presented twice must move money once - which is what
     *                       makes a retry after an unknown outcome safe.
     * @return Approved or Declined - both are known answers
     * @throws PaymentGatewayException when the outcome is unknown
     */
    PaymentResult charge(Card card, int amount, String idempotencyKey);
}
