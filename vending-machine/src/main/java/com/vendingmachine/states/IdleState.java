package com.vendingmachine.states;

import com.vendingmachine.Inventory;
import com.vendingmachine.Slot;

/** Nothing selected, no money held. Carries no payload, so instances are interchangeable. */
public final class IdleState implements State {

    @Override
    public String deniedMessage() {
        return "No item selected.";
    }

    @Override
    public Transition<Void> selectItem(String code, Inventory inventory) {
        Slot slot = inventory.getSlot(code);
        if (slot.isEmpty()) {
            throw new IllegalStateException("Item is sold out.");
        }
        return Transition.to(new ItemSelectedState(slot, 0));
    }

    @Override
    public Transition<Integer> cancel() {
        return new Transition<>(this, 0);
    }
}