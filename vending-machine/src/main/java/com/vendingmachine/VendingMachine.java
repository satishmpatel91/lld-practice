package com.vendingmachine;

import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.CardsNotAcceptedGateway;
import com.vendingmachine.payment.PaymentGateway;
import com.vendingmachine.payment.PaymentResult;
import com.vendingmachine.states.IdleState;
import com.vendingmachine.states.State;
import com.vendingmachine.states.Transition;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Entry point: holds the current state and delegates every operation to it. No
 * transaction data lives here - the selected slot and the money inserted are
 * inside the state.
 *
 * <p>The reference is atomic because two threads must not both read one state
 * and both overwrite it. Pure operations retry on a lost race; a purchase that
 * charges a card never does, because a retried charge is a second charge.
 */
public class VendingMachine {

    /** After this, a later request may take over a claim that is still in flight. */
    static final Duration PAYMENT_TIMEOUT = Duration.ofSeconds(30);

    private final Inventory inventory;
    private final PaymentGateway paymentGateway;
    private final Clock clock;
    private final AtomicReference<State> state = new AtomicReference<>(new IdleState());

    /** A cash-only machine: swiping declines rather than failing. */
    public VendingMachine(Inventory inventory) {
        this(inventory, new CardsNotAcceptedGateway(), Clock.systemUTC());
    }

    public VendingMachine(Inventory inventory, PaymentGateway paymentGateway) {
        this(inventory, paymentGateway, Clock.systemUTC());
    }

    /** Time is injected so the expiry test can move it instead of sleeping. */
    public VendingMachine(Inventory inventory, PaymentGateway paymentGateway, Clock clock) {
        this.inventory = inventory;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    /** Identical in every state, so it never goes through State. */
    public List<ItemView> showItems() {
        List<ItemView> rows = new ArrayList<>();
        for (Slot slot : inventory.allSlots()) {
            rows.add(ItemView.fromSlot(slot));
        }
        return List.copyOf(rows);
    }

    /** Pure, so a lost race costs nothing: recompute against the new state and retry. */
    public void selectItem(String code) {
        expireStaleClaim();
        while (true) {
            State current = state.get();
            Transition<Void> transition = current.selectItem(code, inventory);
            if (state.compareAndSet(current, transition.next())) {
                return;
            }
        }
    }

    public void insertMoney(int amount) {
        expireStaleClaim();
        while (true) {
            State current = state.get();
            Transition<Void> transition = current.insertMoney(amount);
            if (state.compareAndSet(current, transition.next())) {
                return;
            }
        }
    }

    public int cancel() {
        expireStaleClaim();
        while (true) {
            State current = state.get();
            Transition<Integer> transition = current.cancel();
            if (state.compareAndSet(current, transition.next())) {
                return transition.payload();
            }
        }
    }

    /** Cash authorizes locally, but the stock still moves only after the commit is won. */
    public DispenseResult dispense() {
        expireStaleClaim();
        while (true) {
            State current = state.get();
            PaymentResult result = current.authorizeCash();
            if (result instanceof PaymentResult.Declined declined) {
                // not having paid enough is a precondition, not an outcome
                throw new IllegalStateException(declined.reason());
            }
            if (state.compareAndSet(current, current.nextStateFor(result))) {
                return current.releaseItem((PaymentResult.Approved) result);
            }
        }
    }

    /** Three phases. The network call in the middle holds no lock, so a hung gateway freezes nothing. */
    public PurchaseResult swipeCard(Card card) {
        expireStaleClaim();

        // 1 - claim: pure, so losing it leaves nothing to undo
        State current = state.get();
        Transition<Void> claim = current.beginCardPayment(card, clock.instant(), UUID.randomUUID().toString());
        State pending = claim.next();
        if (!state.compareAndSet(current, pending)) {
            return new PurchaseResult.Busy("Another purchase is in progress. Try again in a moment.");
        }

        // 2 - charge: a gateway failure propagates and keeps the claim, so nobody
        //     else can buy an item we may already have been paid for
        PaymentResult charged = pending.authorizeCard(paymentGateway);

        // 3 - commit, then release
        if (!state.compareAndSet(pending, pending.nextStateFor(charged))) {
            return orphaned(charged);
        }
        return switch (charged) {
            case PaymentResult.Approved approved -> {
                DispenseResult out = pending.releaseItem(approved);
                yield new PurchaseResult.Dispensed(out.item(), out.change(), approved.reference());
            }
            case PaymentResult.Declined declined -> new PurchaseResult.Declined(declined.reason());
        };
    }

    /** Lazy expiry: the next request takes over a claim that went stale. */
    private void expireStaleClaim() {
        State current = state.get();
        if (current.isExpired(clock.instant(), PAYMENT_TIMEOUT)) {
            state.compareAndSet(current, new IdleState());
        }
    }

    /** The charge landed after the claim was taken over: money moved, no item to give. */
    private PurchaseResult orphaned(PaymentResult charged) {
        if (charged instanceof PaymentResult.Approved approved) {
            return new PurchaseResult.Busy(
                    "Payment " + approved.reference() + " completed too late and will be refunded.");
        }
        return new PurchaseResult.Declined("The purchase timed out before it completed.");
    }
}
