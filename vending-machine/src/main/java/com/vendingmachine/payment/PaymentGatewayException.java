package com.vendingmachine.payment;

/**
 * The outcome of a charge is <em>unknown</em>: timeout, unreachable host,
 * malformed response. A decline is not this - a decline is a known answer and
 * travels as a PaymentResult.
 *
 * Policy: this propagates. The machine does not advance its state, so nothing
 * is dispensed and the selection survives, but the charge is left unreconciled:
 * the customer may or may not have been billed. Recording and reconciling that
 * attempt is V5 work.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
