package com.vendingmachine.payment;

/** How a purchase is paid for. Nothing downstream of authorize asks which tender it was. */
public interface PaymentMethod {

    PaymentResult authorize(int amount);
}