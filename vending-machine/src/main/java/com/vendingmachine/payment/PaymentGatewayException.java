package com.vendingmachine.payment;

/**
 * The outcome of a charge is <em>unknown</em> - timeout, unreachable, malformed
 * response. A decline is not this: a decline is a known answer and is returned.
 *
 * <p>It propagates and the machine keeps its claim, so nothing is dispensed. The
 * charge is then unreconciled: the customer may or may not have been billed.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
