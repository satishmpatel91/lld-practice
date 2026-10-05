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
 * permits exactly what it overrides - a forgotten guard is impossible.
 *
 * <p>A purchase is not one step: claim, then authorize, then commit. The methods
 * below are grouped that way, and only the pure ones may be retried.
 */
public interface State {

    /** What to tell the customer when an operation is refused here. */
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

    /** Claim the right to charge. Pure: nothing irreversible happens yet. */
    default Transition<Void> beginCardPayment(Card card, Instant startedAt, String attemptId) {
        throw new IllegalStateException(deniedMessage());
    }

    /** A local comparison, so it cannot fail outwardly. */
    default PaymentResult authorizeCash() {
        throw new IllegalStateException(deniedMessage());
    }

    /** Blocks on the network, so the caller must hold no lock while it runs. */
    default PaymentResult authorizeCard(PaymentGateway gateway) {
        throw new IllegalStateException(deniedMessage());
    }

    default State nextStateFor(PaymentResult result) {
        throw new IllegalStateException(deniedMessage());
    }

    /** Only legal for the thread that won the commit - that is what keeps stock safe without a lock. */
    default DispenseResult releaseItem(PaymentResult.Approved approved) {
        throw new IllegalStateException(deniedMessage());
    }

    /** Only a state that waits on someone else can go stale. */
    default boolean isExpired(Instant now, Duration timeout) {
        return false;
    }
}
