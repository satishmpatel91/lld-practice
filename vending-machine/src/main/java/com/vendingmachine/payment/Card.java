package com.vendingmachine.payment;

/** A gateway token, never a card number: a design that cannot hold a PAN cannot leak one. */
public record Card(String token) { }