package com.vendingmachine;

import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.FakePaymentGateway;
import com.vendingmachine.payment.FakePaymentGateway.Behaviour;
import com.vendingmachine.payment.PaymentGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardPurchaseTest {

    private static final int COKE_PRICE = 25;
    private static final Card CARD = new Card("tok_visa_4242");

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.addSlot(new Slot("A1", new Item("Coke", COKE_PRICE), 2));
        inventory.addSlot(new Slot("B1", new Item("Water", 15), 1));
    }

    private VendingMachine machineWith(FakePaymentGateway gateway) {
        return new VendingMachine(inventory, gateway);
    }

    @Nested
    @DisplayName("approved card")
    class Approved {

        private final FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.APPROVE);

        @Test
        @DisplayName("dispenses the item with the gateway's reference and no change")
        void dispensesWithReferenceAndNoChange() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");

            PurchaseResult result = machine.swipeCard(CARD);

            PurchaseResult.Dispensed dispensed = assertInstanceOf(PurchaseResult.Dispensed.class, result);
            assertEquals("Coke", dispensed.item().getName());
            assertEquals(0, dispensed.change(), "a card is charged the exact price, so change is impossible");
            assertEquals("AUTH-1", dispensed.reference());
        }

        @Test
        @DisplayName("charges exactly the price, exactly once")
        void chargesExactlyThePriceOnce() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");

            machine.swipeCard(CARD);

            assertEquals(1, gateway.chargeCount());
            assertEquals(COKE_PRICE, gateway.lastAmount());
            assertEquals(CARD, gateway.lastCard());
        }

        @Test
        @DisplayName("reduces stock by one")
        void reducesStock() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");

            machine.swipeCard(CARD);

            assertEquals(1, inventory.getSlot("A1").getQuantity());
        }

        @Test
        @DisplayName("returns the machine to idle, ready for the next customer")
        void returnsToIdle() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");
            machine.swipeCard(CARD);

            machine.selectItem("B1");
            assertInstanceOf(PurchaseResult.Dispensed.class, machine.swipeCard(CARD));
        }

        @Test
        @DisplayName("cash already inserted is not silently kept by a card purchase")
        void cashInsertedIsNotSwallowed() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");
            machine.insertMoney(10);

            machine.swipeCard(CARD);

            // the 10 is gone from the transaction - the machine is idle and owes nothing
            assertEquals(0, machine.cancel());
        }
    }

    @Nested
    @DisplayName("declined card")
    class Declined {

        private final FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.DECLINE);

        @Test
        @DisplayName("returns a reason instead of throwing")
        void returnsReason() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");

            PurchaseResult result = machine.swipeCard(CARD);

            assertEquals("Card declined by issuer.",
                    assertInstanceOf(PurchaseResult.Declined.class, result).reason());
        }

        @Test
        @DisplayName("dispenses nothing and leaves stock untouched")
        void dispensesNothing() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");

            machine.swipeCard(CARD);

            assertEquals(2, inventory.getSlot("A1").getQuantity());
        }

        @Test
        @DisplayName("keeps the selection so a second card can be tried")
        void keepsSelectionForRetry() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");
            machine.swipeCard(CARD);

            // still ITEM_SELECTED: selecting again is refused, swiping again is not
            assertEquals("An item is already selected.",
                    assertThrows(IllegalStateException.class, () -> machine.selectItem("B1")).getMessage());
            assertInstanceOf(PurchaseResult.Declined.class, machine.swipeCard(new Card("tok_other")));
            assertEquals(2, gateway.chargeCount());
        }

        @Test
        @DisplayName("the user can still pay cash after a decline")
        void cashStillWorksAfterDecline() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");
            machine.swipeCard(CARD);

            machine.insertMoney(COKE_PRICE);

            assertEquals("Coke", machine.dispense().item().getName());
        }
    }

    @Nested
    @DisplayName("gateway failure - outcome unknown")
    class GatewayFailure {

        private final FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.FAIL);

        @Test
        @DisplayName("propagates, because a decline is known and this is not")
        void propagates() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");

            assertThrows(PaymentGatewayException.class, () -> machine.swipeCard(CARD));
        }

        @Test
        @DisplayName("dispenses nothing: no item may leave on an unknown outcome")
        void dispensesNothing() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");

            assertThrows(PaymentGatewayException.class, () -> machine.swipeCard(CARD));

            assertEquals(2, inventory.getSlot("A1").getQuantity());
        }

        @Test
        @DisplayName("does not advance the state, so the selection and any cash survive")
        void doesNotAdvanceState() {
            VendingMachine machine = machineWith(gateway);
            machine.selectItem("A1");
            machine.insertMoney(10);

            assertThrows(PaymentGatewayException.class, () -> machine.swipeCard(CARD));

            assertEquals(10, machine.cancel(), "the cash must still be refundable");
        }
    }

    @Nested
    @DisplayName("illegal sequences")
    class IllegalSequences {

        @Test
        @DisplayName("swiping with nothing selected is refused")
        void swipeWithoutSelection() {
            VendingMachine machine = machineWith(new FakePaymentGateway(Behaviour.APPROVE));

            assertEquals("No item selected.",
                    assertThrows(IllegalStateException.class, () -> machine.swipeCard(CARD)).getMessage());
        }

        @Test
        @DisplayName("a refused swipe never reaches the gateway")
        void refusedSwipeNeverCharges() {
            FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.APPROVE);
            VendingMachine machine = machineWith(gateway);

            assertThrows(IllegalStateException.class, () -> machine.swipeCard(CARD));

            assertEquals(0, gateway.chargeCount());
        }
    }

    @Nested
    @DisplayName("cash-only machine")
    class CashOnlyMachine {

        @Test
        @DisplayName("declines a swipe rather than failing")
        void declinesSwipe() {
            VendingMachine machine = new VendingMachine(inventory);
            machine.selectItem("A1");

            PurchaseResult result = machine.swipeCard(CARD);

            assertEquals("Card payments are not available.",
                    assertInstanceOf(PurchaseResult.Declined.class, result).reason());
        }

        @Test
        @DisplayName("still sells for cash")
        void stillSellsForCash() {
            VendingMachine machine = new VendingMachine(inventory);
            machine.selectItem("A1");
            machine.insertMoney(30);

            assertEquals(5, machine.dispense().change());
        }
    }
}
