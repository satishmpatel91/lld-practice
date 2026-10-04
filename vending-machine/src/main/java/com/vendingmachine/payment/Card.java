package com.vendingmachine.payment;

/**
 * A card, represented by a gateway token. Never model the real PAN: a design
 * that cannot hold a card number cannot leak one.
 */
public record Card(String token) { }