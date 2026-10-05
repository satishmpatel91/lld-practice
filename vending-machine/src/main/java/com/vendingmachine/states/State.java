package com.vendingmachine.states;

import com.vendingmachine.DispenseResult;
import com.vendingmachine.Inventory;
import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.PaymentGateway;
import com.vendingmachine.payment.PaymentResult;

import java.time.Duration;
import java.time.Instant;

/**
 * One state of the machine. Every operation is denied by default, so a state
 * permits exactly what it overrides - a forgotten guard is impossible, and the
 * compiler ties each method name to its operation.
 *
 * <p>Since V4 the interface is split by <em>phase</em>, not just by operation,
 * because a purchase can no longer be one indivisible step:
 * <ol>
 *   <li>pure claim      - {@link #beginCardPayment}, {@link #selectItem}, {@link #insertMoney}
 *   <li>authorization   - {@link #authorizeCash}, {@link #authorizeCard}
 *   <li>commit          - {@link #nextStateFor} (pure), then {@link #releaseItem} (side effect)
 * </ol>
 * The caller may only call {@link #releaseItem} once it has won the commit, which
 * is what keeps the stock safe without locking it.
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

    default Transition<Integer> cancel() {
        throw new IllegalStateException(deniedMessage());
    }

    /** Phase 1 for a card: pure, so losing the race costs nothing and undoes nothing. */
    default Transition<Void> beginCardPayment(Card card, Instant startedAt, String attemptId) {
        throw new IllegalStateException(deniedMessage());
    }

    /** Phase 2 for cash: a local comparison that cannot fail outwardly. */
    default PaymentResult authorizeCash() {
        throw new IllegalStateException(deniedMessage());
    }

    /** Phase 2 for a card: blocks on the network, so the caller holds nothing while it runs. */
    default PaymentResult authorizeCard(PaymentGateway gateway) {
        throw new IllegalStateException(deniedMessage());
    }

    /** Phase 3a, pure: where the machine goes, given the answer. */
    default State nextStateFor(PaymentResult result) {
        throw new IllegalStateException(deniedMessage());
    }

    /** Phase 3b: hand the item over. Only legal for the thread that won the commit. */
    default DispenseResult releaseItem(PaymentResult.Approved approved) {
        throw new IllegalStateException(deniedMessage());
    }

    /** Only a state that waits on someone else can go stale, so everything else answers no. */
    default boolean isExpired(Instant now, Duration timeout) {
        return false;
    }
}
