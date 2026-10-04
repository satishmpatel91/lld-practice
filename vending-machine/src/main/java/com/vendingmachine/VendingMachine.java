package com.vendingmachine;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class VendingMachine {
    private final Inventory inventory;
    private Slot selectedSlot;
    private int amountInserted;

    private MachineState state() {
        if (selectedSlot == null) {
            return MachineState.IDLE;
        } else {
            return MachineState.ITEM_SELECTED;
        }
    }

    private void requireState(MachineState expected, String message) {
        if (state() != expected) {
            throw new IllegalStateException(message);
        }
    }

    public List<ItemView> showItems() {
        return inventory.allSlots().stream()
                .map(ItemView::fromSlot)
                .toList();
    }

    public void selectItem(String code) {
        requireState(MachineState.IDLE, "An item is already selected.");
        Slot slot = inventory.getSlot(code);
        if (slot.isEmpty()) {
            throw new IllegalStateException("Item is sold out.");
        }
        selectedSlot = slot;
    }

    public void insertMoney(int amount) {
        requireState(MachineState.ITEM_SELECTED, "No item selected.");
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to insert must be positive.");
        }
        amountInserted += amount;
    }

    public DispenseResult dispense() {
        requireState(MachineState.ITEM_SELECTED, "No item selected.");
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
        if (state() == MachineState.IDLE) {
            return 0;
        }
        int refund = amountInserted;
        selectedSlot = null;
        amountInserted = 0;
        return refund;
    }
}