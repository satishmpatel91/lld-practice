package com.vendingmachine.payment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scripted stand-in for a real gateway: a test must be able to demand a
 * decline or a failure, which no real gateway allows. Test-only, so no fake
 * ships in production code.
 *
 * <p>It records what it was asked, because the properties worth asserting are
 * "charged exactly once" and "charged exactly the price" - neither of which a
 * return value can show. It is also <strong>idempotent</strong>, like a real
 * gateway: a key it has seen before returns the stored answer and moves no
 * money, so {@link #chargeCount()} counts money movements rather than calls.
 */
public final class FakePaymentGateway implements PaymentGateway {

    public enum Behaviour { APPROVE, DECLINE, FAIL }

    private final Behaviour behaviour;
    private final Map<String, PaymentResult> answered = new ConcurrentHashMap<>();
    private final AtomicInteger chargeCount = new AtomicInteger();
    private final AtomicInteger callCount = new AtomicInteger();

    private volatile int lastAmount;
    private volatile Card lastCard;

    public FakePaymentGateway(Behaviour behaviour) {
        this.behaviour = behaviour;
    }

    @Override
    public PaymentResult charge(Card card, int amount, String idempotencyKey) {
        callCount.incrementAndGet();
        lastAmount = amount;
        lastCard = card;

        PaymentResult already = answered.get(idempotencyKey);
        if (already != null) {
            return already;      // same attempt: answer again, charge nothing
        }
        if (behaviour == Behaviour.FAIL) {
            throw new PaymentGatewayException("Gateway unreachable.");
        }
        PaymentResult result = behaviour == Behaviour.APPROVE
                ? new PaymentResult.Approved(0, "AUTH-" + chargeCount.incrementAndGet())
                : new PaymentResult.Declined("Card declined by issuer.");
        if (behaviour == Behaviour.APPROVE) {
            answered.put(idempotencyKey, result);
        }
        return result;
    }

    /** How many times money actually moved. */
    public int chargeCount() {
        return chargeCount.get();
    }

    /** How many times the gateway was called, including repeats of a known key. */
    public int callCount() {
        return callCount.get();
    }

    public int lastAmount() {
        return lastAmount;
    }

    public Card lastCard() {
        return lastCard;
    }
}
