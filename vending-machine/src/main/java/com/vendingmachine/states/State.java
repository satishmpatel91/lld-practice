package com.vendingmachine.states;

import com.vendingmachine.DispenseResult;
import com.vendingmachine.Inventory;
import com.vendingmachine.PurchaseResult;
import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.PaymentGateway;

/**
 * One state of the machine. Every operation is denied by default, so a state
 * permits exactly what it overrides - a forgotten guard is impossible, and the
 * compiler ties each method name to its operation.
 */
public interface State {

    /** What to tell the user when an operation is not allowed here. */
    String deniedMessage();

    default Transition<Void> selectItem(String code, Inventory inventory) {
        throw new IllegalStateException(deniedMessage());
    }

    default Transition<Void> insertMoney(int amount) {
        throw new IllegalStateException(deniedMessage());
    }

    default Transition<DispenseResult> dispense() {
        throw new IllegalStateException(deniedMessage());
    }

    default Transition<PurchaseResult> swipeCard(Card card, PaymentGateway gateway) {
        throw new IllegalStateException(deniedMessage());
    }

    default Transition<Integer> cancel() {
        throw new IllegalStateException(deniedMessage());
    }
}
