package com.vendingmachine.payment;

/**
 * Scripted stand-in for a real gateway: a test must be able to demand a
 * decline or a failure, which no real gateway allows. Test-only, so no fake
 * ships in production code.
 *
 * It records what it was asked, because the properties worth asserting are
 * "charged exactly once" and "charged exactly the price" - neither of which a
 * return value can show.
 */
public final class FakePaymentGateway implements PaymentGateway {

    public enum Behaviour { APPROVE, DECLINE, FAIL }

    private final Behaviour behaviour;

    private int chargeCount;
    private int lastAmount;
    private Card lastCard;

    public FakePaymentGateway(Behaviour behaviour) {
        this.behaviour = behaviour;
    }

    @Override
    public PaymentResult charge(Card card, int amount) {
        chargeCount++;
        lastAmount = amount;
        lastCard = card;
        return switch (behaviour) {
            case APPROVE -> new PaymentResult.Approved(0, "AUTH-" + chargeCount);
            case DECLINE -> new PaymentResult.Declined("Card declined by issuer.");
            case FAIL -> throw new PaymentGatewayException("Gateway unreachable.");
        };
    }

    public int chargeCount() {
        return chargeCount;
    }

    public int lastAmount() {
        return lastAmount;
    }

    public Card lastCard() {
        return lastCard;
    }
}
