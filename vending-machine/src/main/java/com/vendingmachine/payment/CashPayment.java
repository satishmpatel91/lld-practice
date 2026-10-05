package com.vendingmachine.payment;

/**
 * Cash already inside the machine. Treating its local comparison as an
 * authorization is what lets both tenders share one pipeline.
 */
public final class CashPayment implements PaymentMethod {

    private final int amountInserted;

    public CashPayment(int amountInserted) {
        this.amountInserted = amountInserted;
    }

    @Override
    public PaymentResult authorize(int amount) {
        if (amountInserted < amount) {
            return new PaymentResult.Declined("Insufficient funds. Please insert more money.");
        }
        return new PaymentResult.Approved(amountInserted - amount, "CASH");
    }
}
