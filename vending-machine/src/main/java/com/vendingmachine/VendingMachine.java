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
 * Owns the inventory, the card gateway, the clock and the current state, and
 * delegates every operation to that state. It holds no transaction data of its
 * own - the selected slot and the money inserted live inside the state.
 *
 * <p>Since V4 the state is an {@link AtomicReference}, which closes two holes a
 * plain field left open: the lost update when two threads both read the same
 * state and both assign, and the visibility hole where one thread never sees
 * the other's write at all.
 *
 * <p>Operations whose computation is <strong>pure</strong> use a retry loop:
 * losing the race costs nothing, so recomputing is free. Operations that charge
 * a card or release an item never retry - a retried charge is a second charge.
 * Those run in three phases: claim by compare-and-set, act, then commit by
 * compare-and-set.
 */
public class VendingMachine {

    /** How long a card charge may be in flight before a later request may take over. */
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

    /** Time is a dependency, so the expiry test advances a clock instead of sleeping. */
    public VendingMachine(Inventory inventory, PaymentGateway paymentGateway, Clock clock) {
        this.inventory = inventory;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    /** Legal in every state and identical in all of them, so it never goes through State. */
    public List<ItemView> showItems() {
        List<ItemView> rows = new ArrayList<>();
        for (Slot slot : inventory.allSlots()) {
            rows.add(ItemView.fromSlot(slot));
        }
        return List.copyOf(rows);
    }

    /**
     * Pure, so losing the race costs nothing and recomputing is free: read the
     * state, ask it for the next one, and publish only if nobody moved it first.
     */
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

    /**
     * Cash. Authorization is local, so there is no claim to hold and no window
     * to be refused in - but the stock still moves only after the commit is won.
     */
    public DispenseResult dispense() {
        expireStaleClaim();
        while (true) {
            State current = state.get();
            PaymentResult result = current.authorizeCash();          // pure
            if (result instanceof PaymentResult.Declined declined) {
                /* "you have not paid enough yet" is a precondition, not an outcome */
                throw new IllegalStateException(declined.reason());
            }
            if (state.compareAndSet(current, current.nextStateFor(result))) {
                return current.releaseItem((PaymentResult.Approved) result);
            }
            /* someone else moved the machine on; recompute against the new state */
        }
    }

    /**
     * Card. Three phases, and the middle one - the network call - is made while
     * holding no lock, so one hung gateway cannot freeze the whole machine.
     */
    public PurchaseResult swipeCard(Card card) {
        expireStaleClaim();

        // phase 1: claim. Pure, so losing it costs nothing and undoes nothing.
        State current = state.get();
        Transition<Void> claim = current.beginCardPayment(card, clock.instant(), UUID.randomUUID().toString());
        State pending = claim.next();
        if (!state.compareAndSet(current, pending)) {
            return new PurchaseResult.Busy("Another purchase is in progress. Try again in a moment.");
        }

        // phase 2: the charge. Holding nothing. A PaymentGatewayException propagates
        // and deliberately leaves the claim in place, so nobody else can buy an item
        // we may already have been paid for.
        PaymentResult charged = pending.authorizeCard(paymentGateway);

        // phase 3: commit, then act
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

    /**
     * Lazy expiry: the next request notices a stale claim and takes it over. A
     * background sweeper would also work, and would be worth adding so that
     * monitoring can see a machine stuck here - but it is not needed for
     * correctness, because nobody is inconvenienced by a machine nobody is using.
     */
    private void expireStaleClaim() {
        State current = state.get();
        if (current.isExpired(clock.instant(), PAYMENT_TIMEOUT)) {

            state.compareAndSet(current, new IdleState());
        }
    }

    /**
     * The charge completed but the claim had already been expired and taken over,
     * so this thread has money that moved and no right to release an item.
     */
    private PurchaseResult orphaned(PaymentResult charged) {

        if (charged instanceof PaymentResult.Approved approved) {
            return new PurchaseResult.Busy(
                    "Payment " + approved.reference() + " completed too late and will be refunded.");
        }
        return new PurchaseResult.Declined("The purchase timed out before it completed.");
    }
}
