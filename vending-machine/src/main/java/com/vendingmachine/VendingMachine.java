package com.vendingmachine;

import com.vendingmachine.states.IdleState;
import com.vendingmachine.states.State;
import com.vendingmachine.states.Transition;

import java.util.List;

/**
 * Thin now: owns the inventory and the current state, and delegates every
 * operation to that state. It holds no transaction data of its own - the
 * selected slot and the money inserted live in ItemSelectedState, the only
 * place they ever meant anything.
 */
public class VendingMachine {

    private final Inventory inventory;
    private State state = new IdleState();

    public VendingMachine(Inventory inventory) {
        this.inventory = inventory;
    }

    /** Legal in every state and identical in all of them, so it never goes through State. */
    public List<ItemView> showItems() {
        return inventory.allSlots().stream()
                .map(ItemView::fromSlot)
                .toList();
    }

    public void selectItem(String code) {
        state = state.selectItem(code, inventory).next();
    }

    public void insertMoney(int amount) {
        state = state.insertMoney(amount).next();
    }

    public DispenseResult dispense() {
        Transition<DispenseResult> transition = state.dispense();
        state = transition.next();
        return transition.payload();
    }

    public int cancel() {
        Transition<Integer> transition = state.cancel();
        state = transition.next();
        return transition.payload();
    }
}
