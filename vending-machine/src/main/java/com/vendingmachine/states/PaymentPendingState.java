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
 * A card charge is in flight. This state <em>is</em> the claim: whoever put the
 * machine here holds the exclusive right to charge and to release the item.
 *
 * <p>{@code startedAt} lets a later request notice the claim went stale;
 * {@code attemptId} is the idempotency key, so a retried charge moves money once.
 * Only the card phases are overridden, so everything else is refused while the
 * charge is out - which is correct, because nothing can be answered until the
 * gateway replies.
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
     * Charges and nothing else: the caller cannot yet know whether it still holds
     * the claim, so no state and no stock may move here.
     */
    @Override
    public PaymentResult authorizeCard(PaymentGateway gateway) {
        return new CardPayment(card, gateway, attemptId).authorize(slot.getItem().getPrice());
    }

    @Override
    public State nextStateFor(PaymentResult result) {
        return switch (result) {
            case PaymentResult.Approved approved -> new IdleState();
            // back to the selection, cash intact: try another card, or pay cash
            case PaymentResult.Declined declined -> new ItemSelectedState(slot, amountInserted);
        };
    }

    @Override
    public DispenseResult releaseItem(PaymentResult.Approved approved) {
        Item item = slot.getItem();
        slot.reduceQuantity();
        return new DispenseResult(item, approved.change());
    }
}
