package com.vendingmachine.states;

import com.vendingmachine.DispenseResult;
import com.vendingmachine.Item;
import com.vendingmachine.PurchaseResult;
import com.vendingmachine.Slot;
import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.CardPayment;
import com.vendingmachine.payment.CashPayment;
import com.vendingmachine.payment.PaymentGateway;
import com.vendingmachine.payment.PaymentResult;

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

    /**
     * Cash goes through the same authorization pipeline as a card. Its decline
     * is a precondition violation - "you have not paid yet" - so it throws,
     * which is the contract every version since V1 has kept.
     */
    @Override
    public Transition<DispenseResult> dispense() {
        Item item = slot.getItem();
        PaymentResult result = new CashPayment(amountInserted).authorize(item.getPrice());
        return switch (result) {
            case PaymentResult.Approved approved -> {
                slot.reduceQuantity();
                yield new Transition<>(new IdleState(), new DispenseResult(item, approved.change()));
            }
            case PaymentResult.Declined declined -> throw new IllegalStateException(declined.reason());
        };
    }

    /**
     * A card decline is an outcome, not a precondition violation, so it is
     * returned rather than thrown - and the machine stays here, so the user can
     * try another card instead of re-selecting their item.
     */
    @Override
    public Transition<PurchaseResult> swipeCard(Card card, PaymentGateway gateway) {
        Item item = slot.getItem();
        PaymentResult result = new CardPayment(card, gateway).authorize(item.getPrice());
        return switch (result) {
            case PaymentResult.Approved approved -> {
                slot.reduceQuantity();
                PurchaseResult dispensed =
                        new PurchaseResult.Dispensed(item, approved.change(), approved.reference());
                yield new Transition<>(new IdleState(), dispensed);
            }
            case PaymentResult.Declined declined ->
                    new Transition<>(this, new PurchaseResult.Declined(declined.reason()));
        };
    }

    @Override
    public Transition<Integer> cancel() {
        return new Transition<>(new IdleState(), amountInserted);
    }
}
