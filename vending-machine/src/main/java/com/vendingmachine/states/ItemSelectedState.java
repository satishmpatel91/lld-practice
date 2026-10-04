package com.vendingmachine.states;

import com.vendingmachine.DispenseResult;
import com.vendingmachine.Item;
import com.vendingmachine.Slot;

/** A slot is selected and some money may be held. Immutable: every change is a new instance. */
public final class ItemSelectedState implements State {

    private final Slot slot;
    private final int amountInserted;

    public ItemSelectedState(Slot slot, int amountInserted) {
        this.slot = slot;
        this.amountInserted = amountInserted;
    }

    @Override
    public String deniedMessage() {
        return "An item is already selected.";
    }

    @Override
    public Transition<Void> insertMoney(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to insert must be positive.");
        }
        return Transition.to(new ItemSelectedState(slot, amountInserted + amount));
    }

    @Override
    public Transition<DispenseResult> dispense() {
        Item item = slot.getItem();
        if (amountInserted < item.getPrice()) {
            throw new IllegalStateException("Insufficient funds. Please insert more money.");
        }
        slot.reduceQuantity();
        DispenseResult result = new DispenseResult(item, amountInserted - item.getPrice());
        return new Transition<>(new IdleState(), result);
    }

    @Override
    public Transition<Integer> cancel() {
        return new Transition<>(new IdleState(), amountInserted);
    }
}