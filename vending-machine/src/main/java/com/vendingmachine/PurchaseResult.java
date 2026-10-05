package com.vendingmachine;

/**
 * What a card attempt produced. Sealed, so adding a case makes every switch over
 * it fail to compile until that case is handled.
 */
public sealed interface PurchaseResult
        permits PurchaseResult.Dispensed, PurchaseResult.Declined, PurchaseResult.Busy {

    record Dispensed(Item item, int change, String reference) implements PurchaseResult { }

    /** The gateway said no. Retrying the same card is pointless. */
    record Declined(String reason) implements PurchaseResult { }

    /** Someone else holds the in-flight transaction. Retrying in a moment is sensible. */
    record Busy(String reason) implements PurchaseResult { }
}
