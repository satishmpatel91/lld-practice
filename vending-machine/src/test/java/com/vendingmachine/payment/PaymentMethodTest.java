package com.vendingmachine.payment;

import com.vendingmachine.payment.FakePaymentGateway.Behaviour;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The strategies in isolation, with no machine and no state involved. */
class PaymentMethodTest {

    private static final Card CARD = new Card("tok_visa_4242");

    @Test
    @DisplayName("cash approves when enough is held, and reports the change")
    void cashApprovesWithChange() {
        PaymentResult result = new CashPayment(50).authorize(25);

        PaymentResult.Approved approved = assertInstanceOf(PaymentResult.Approved.class, result);
        assertEquals(25, approved.change());
        assertEquals("CASH", approved.reference());
    }

    @Test
    @DisplayName("cash approves an exact amount with no change")
    void cashApprovesExactAmount() {
        assertEquals(0, assertInstanceOf(PaymentResult.Approved.class, new CashPayment(25).authorize(25)).change());
    }

    @Test
    @DisplayName("cash declines when short, with the message the machine has used since V1")
    void cashDeclinesWhenShort() {
        PaymentResult result = new CashPayment(10).authorize(25);

        assertEquals("Insufficient funds. Please insert more money.",
                assertInstanceOf(PaymentResult.Declined.class, result).reason());
    }

    @Test
    @DisplayName("card normalises change to zero even if a gateway claims otherwise")
    void cardNeverReturnsChange() {
        PaymentGateway overpayingGateway = (card, amount, key) -> new PaymentResult.Approved(99, "AUTH-X");

        PaymentResult result = new CardPayment(CARD, overpayingGateway, "key-1").authorize(25);

        assertEquals(0, assertInstanceOf(PaymentResult.Approved.class, result).change());
    }

    @Test
    @DisplayName("card passes the decline reason through untouched")
    void cardPassesDeclineThrough() {
        PaymentResult result = new CardPayment(CARD, new FakePaymentGateway(Behaviour.DECLINE), "key-2").authorize(25);

        assertEquals("Card declined by issuer.",
                assertInstanceOf(PaymentResult.Declined.class, result).reason());
    }

    @Test
    @DisplayName("card lets a gateway failure propagate rather than calling it a decline")
    void cardPropagatesGatewayFailure() {
        CardPayment payment = new CardPayment(CARD, new FakePaymentGateway(Behaviour.FAIL), "key-3");

        assertThrows(PaymentGatewayException.class, () -> payment.authorize(25));
    }

    @Test
    @DisplayName("the same attempt presented twice moves money once")
    void sameAttemptChargesOnce() {
        FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.APPROVE);
        CardPayment payment = new CardPayment(CARD, gateway, "attempt-7");

        PaymentResult first = payment.authorize(25);
        PaymentResult second = payment.authorize(25);

        assertEquals(2, gateway.callCount(), "the gateway was asked twice");
        assertEquals(1, gateway.chargeCount(), "but money moved once");
        assertEquals(assertInstanceOf(PaymentResult.Approved.class, first).reference(),
                assertInstanceOf(PaymentResult.Approved.class, second).reference(),
                "the same attempt gets the same reference back");
    }

    @Test
    @DisplayName("a different attempt is a different charge")
    void differentAttemptChargesAgain() {
        FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.APPROVE);

        new CardPayment(CARD, gateway, "attempt-1").authorize(25);
        new CardPayment(CARD, gateway, "attempt-2").authorize(25);

        assertEquals(2, gateway.chargeCount());
    }

    @Test
    @DisplayName("card refuses a non-positive amount before touching the gateway")
    void cardRefusesNonPositiveAmount() {
        FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.APPROVE);

        assertThrows(IllegalArgumentException.class, () -> new CardPayment(CARD, gateway, "key-4").authorize(0));

        assertEquals(0, gateway.chargeCount());
    }
}
