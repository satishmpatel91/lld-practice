package com.vendingmachine.states;

import com.vendingmachine.DispenseResult;
import com.vendingmachine.Item;
import com.vendingmachine.Slot;
import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.CashPayment;
import com.vendingmachine.payment.PaymentResult;

import java.time.Instant;

/**
 * A slot is selected and some cash may be held. Immutable, so a new transaction
 * cannot inherit the last one's money - there is nothing to reset.
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

    @Override
    public Transition<Void> beginCardPayment(Card card, Instant startedAt, String attemptId) {
        return Transition.to(new PaymentPendingState(slot, amountInserted, card, startedAt, attemptId));
    }

    /** Cash uses the same pipeline as a card; only this authorization is local. */
    @Override
    public PaymentResult authorizeCash() {
        return new CashPayment(amountInserted).authorize(slot.getItem().getPrice());
    }

    @Override
    public State nextStateFor(PaymentResult result) {
        return switch (result) {
            case PaymentResult.Approved approved -> new IdleState();
            // stay put: the money is still in the machine, so the customer can top up
            case PaymentResult.Declined declined -> this;
        };
    }

    /** Reached only by the commit winner, which is why quantity needs no lock. */
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
