package com.vendingmachine.states;

import com.vendingmachine.DispenseResult;
import com.vendingmachine.Item;
import com.vendingmachine.Slot;
import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.CashPayment;
import com.vendingmachine.payment.PaymentResult;

import java.time.Instant;

/**
 * A slot is selected and some cash may be held. Immutable: every change is a
 * new instance, so a fresh transaction cannot inherit the previous one's money.
 */
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

    /** Pure: nothing irreversible happens when a card payment is claimed. */
    @Override
    public Transition<Void> beginCardPayment(Card card, Instant startedAt, String attemptId) {
        return Transition.to(new PaymentPendingState(slot, amountInserted, card, startedAt, attemptId));
    }

    /**
     * Cash goes through the same authorization pipeline as a card, and its
     * authorization is local, so there is nothing to wait for and no claim to hold.
     */
    @Override
    public PaymentResult authorizeCash() {
        return new CashPayment(amountInserted).authorize(slot.getItem().getPrice());
    }

    @Override
    public State nextStateFor(PaymentResult result) {
        return switch (result) {
            case PaymentResult.Approved approved -> new IdleState();
            /* the money stays in the machine: the customer can top up and try again */
            case PaymentResult.Declined declined -> this;
        };
    }

    /**
     * Phase 3b. Reached only by the thread that won the commit, which is why the
     * quantity needs no lock and no atomic of its own.
     */
    @Override
    public DispenseResult releaseItem(PaymentResult.Approved approved) {
        Item item = slot.getItem();
        slot.reduceQuantity();
        return new DispenseResult(item, approved.change());
    }

    @Override
    public Transition<Integer> cancel() {
        return new Transition<>(new IdleState(), amountInserted);
    }
}
