package com.vendingmachine;

import lombok.Getter;

/** A physical position such as A1, stocking one item type in some quantity. */
@Getter
public class Slot {
    private final String code;
    private final Item item;
    private int quantity;

    public Slot(String code, Item item, int quantity) {
        this.code = code;
        this.item = item;
        this.quantity = quantity;
    }

    public boolean isEmpty() {
        return quantity <= 0;
    }

    public void reduceQuantity() {
        if (quantity <= 0) {
            throw new IllegalStateException("Cannot reduce quantity. Slot is empty.");
        }
        quantity--;
    }
}
