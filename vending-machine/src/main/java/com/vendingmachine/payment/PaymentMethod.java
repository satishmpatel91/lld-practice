package com.vendingmachine.payment;

/**
 * How a purchase is paid for. Cash is the degenerate case: its authorization is
 * instantaneous, so nothing downstream ever asks which tender it received.
 */
public interface PaymentMethod {

    PaymentResult authorize(int amount);
}