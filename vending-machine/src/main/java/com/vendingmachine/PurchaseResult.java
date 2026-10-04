package com.vendingmachine;

/** What a card purchase attempt produced: the goods, or a reason it did not happen. */
public sealed interface PurchaseResult permits PurchaseResult.Dispensed, PurchaseResult.Declined {

    record Dispensed(Item item, int change, String reference) implements PurchaseResult { }

    record Declined(String reason) implements PurchaseResult { }
}
