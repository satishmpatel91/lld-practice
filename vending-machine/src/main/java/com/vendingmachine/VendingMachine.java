package com.vendingmachine;

import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.vendingmachine.Operation.*;

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

    public List<ItemView> showItems() {
        state().requireOperation(SHOW_ITEMS, "Items cannot be shown right now.");
        return inventory.allSlots().stream()
                .map(ItemView::fromSlot)
                .toList();
    }

    public void selectItem(String code) {
        state().requireOperation(SELECT_ITEM, "An item is already selected.");
        Slot slot = inventory.getSlot(code);
        if (slot.isEmpty()) {
            throw new IllegalStateException("Item is sold out.");
        }
        selectedSlot = slot;
    }

    public void insertMoney(int amount) {
        state().requireOperation(INSERT_MONEY, "No item selected.");
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to insert must be positive.");
        }
        amountInserted += amount;
    }

    public DispenseResult dispense() {
        state().requireOperation(DISPENSE, "No item selected.");
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
        state().requireOperation(CANCEL, "No item selected.");
        int refund = amountInserted;
        selectedSlot = null;
        amountInserted = 0;
        return refund;
    }
}