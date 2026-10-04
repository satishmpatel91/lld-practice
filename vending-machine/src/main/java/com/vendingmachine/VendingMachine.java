package com.vendingmachine;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class VendingMachine {
    private final Inventory inventory;
    private Slot selectedSlot;
    private int amountInserted;

    public List<ItemView> showItems() {
        return inventory.allSlots().stream()
                .map(ItemView::fromSlot)
                .toList();
    }

    public void selectItem(String code) {
        if (selectedSlot != null) {
            throw new IllegalStateException("An item is already selected.");
        }
        Slot slot = inventory.getSlot(code);
        if (slot.isEmpty()) {
            throw new IllegalStateException("Item is sold out.");
        }
        selectedSlot = slot;
    }

    public void insertMoney(int amount) {
        if (selectedSlot == null) {
            throw new IllegalStateException("No item selected.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to insert must be positive.");
        }
        amountInserted += amount;
    }

    public DispenseResult dispense() {
        if (selectedSlot == null) {
            throw new IllegalStateException("No item selected.");
        }
        Item item = selectedSlot.getItem();
        if (amountInserted < item.getPrice()) {
            throw new IllegalStateException("Insufficient funds. Please insert more money.");
        }
        DispenseResult result = new DispenseResult(item, amountInserted - item.getPrice());
        selectedSlot.reduceQuantity();
        selectedSlot = null;
        amountInserted = 0;
        return result;
    }

    public int cancel() {
        if (selectedSlot == null) {
            return 0;
        }
        int refund = amountInserted;
        selectedSlot = null;
        amountInserted = 0;
        return refund;
    }
}