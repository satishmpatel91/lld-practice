package com.vendingmachine.payment;

/**
 * The external world, declared by the domain that needs it (dependency
 * inversion). A real implementation wraps a bank SDK; tests use a scripted
 * stand-in, since no real gateway can be told to decline on demand.
 */
public interface PaymentGateway {

    /**
     * @return Approved or Declined - both are known answers
     * @throws PaymentGatewayException when the outcome is unknown
     */
    PaymentResult charge(Card card, int amount);
}
