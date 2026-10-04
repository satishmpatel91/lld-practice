package com.vendingmachine.payment;

/** The outcome of one authorization attempt. */
public sealed interface PaymentResult permits PaymentResult.Approved, PaymentResult.Declined {

    /** @param reference the gateway's id for this charge - V4 will need it for idempotency. */
    record Approved(int change, String reference) implements PaymentResult { }

    record Declined(String reason) implements PaymentResult { }
}