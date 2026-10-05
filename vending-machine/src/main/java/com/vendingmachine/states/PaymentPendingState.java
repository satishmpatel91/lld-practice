package com.vendingmachine.states;

import com.vendingmachine.DispenseResult;
import com.vendingmachine.Item;
import com.vendingmachine.Slot;
import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.CardPayment;
import com.vendingmachine.payment.PaymentGateway;
import com.vendingmachine.payment.PaymentResult;

import java.time.Duration;
import java.time.Instant;

/**
 * A card charge is in flight. This state is the <strong>claim</strong>: whoever
 * put the machine here has won the exclusive right to charge and, if the charge
 * is approved and the commit still holds, to release the item.
 *
 * <p>It carries {@code startedAt} so a later request can notice the claim went
 * stale, and {@code attemptId} so the charge is idempotent: a retried attempt
 * presents the same key and moves money once.
 *
 * <p>Nothing is overridden except the card phases, so by deny-by-default this
 * state refuses selection, cash, and cancellation while the charge is out.
 */
public final class PaymentPendingState implements State {

    private final Slot slot;
    private final int amountInserted;
    private final Card card;
    private final Instant startedAt;
    private final String attemptId;

    public PaymentPendingState(Slot slot, int amountInserted, Card card, Instant startedAt, String attemptId) {
        this.slot = slot;
        this.amountInserted = amountInserted;
        this.card = card;
        this.startedAt = startedAt;
        this.attemptId = attemptId;
    }

    @Override
    public String deniedMessage() {
        return "A payment is already in progress.";
    }

    @Override
    public boolean isExpired(Instant now, Duration timeout) {
        return startedAt.plus(timeout).isBefore(now);
    }

    /**
     * Phase 2. Charges the card and does nothing else: at this point the caller
     * cannot yet know whether it still holds the claim, so no state and no stock
     * may move. A gateway failure propagates and leaves this state in place, so
     * no second customer can buy an item we may already have been paid for.
     */
    @Override
    public PaymentResult authorizeCard(PaymentGateway gateway) {
        return new CardPayment(card, gateway, attemptId).authorize(slot.getItem().getPrice());
    }

    @Override
    public State nextStateFor(PaymentResult result) {
        return switch (result) {
            case PaymentResult.Approved approved -> new IdleState();
            /* back to the selection with the cash intact: try another card, or pay cash */
            case PaymentResult.Declined declined -> new ItemSelectedState(slot, amountInserted);
        };
    }

    @Override
    public DispenseResult releaseItem(PaymentResult.Approved approved) {
        Item item = slot.getItem();
        slot.reduceQuantity();
        return new DispenseResult(item, approved.change());
    }

    /** Identifies the attempt, for the idempotency key and for reconciling an abandoned claim. */
    public String attemptId() {
        return attemptId;
    }

    /** What the customer is owed if this claim is abandoned after the money moved. */
    public int amountInserted() {
        return amountInserted;
    }
}
