package com.vendingmachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendingMachineTest {

    private static final int COKE_PRICE = 25;
    private static final int WATER_PRICE = 15;

    private Inventory inventory;
    private VendingMachine machine;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.addSlot(new Slot("A1", new Item("Coke", COKE_PRICE), 2));
        inventory.addSlot(new Slot("A2", new Item("Chips", 20), 0));
        inventory.addSlot(new Slot("B1", new Item("Water", WATER_PRICE), 1));
        machine = new VendingMachine(inventory);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("dispenses the selected item and returns the change")
        void dispensesSelectedItemWithChange() {
            machine.selectItem("A1");
            machine.insertMoney(50);

            DispenseResult result = machine.dispense();

            assertEquals("Coke", result.item().getName());
            assertEquals(50 - COKE_PRICE, result.change());
        }

        @Test
        @DisplayName("dispenses the item that was actually selected, not the first slot")
        void dispensesTheItemSelected() {
            machine.selectItem("B1");
            machine.insertMoney(20);

            DispenseResult result = machine.dispense();

            assertEquals("Water", result.item().getName());
            assertEquals(20 - WATER_PRICE, result.change());
        }

        @Test
        @DisplayName("exact payment leaves no change")
        void exactPaymentLeavesNoChange() {
            machine.selectItem("A1");
            machine.insertMoney(COKE_PRICE);

            assertEquals(0, machine.dispense().change());
        }

        @Test
        @DisplayName("money inserted in several coins accumulates")
        void insertsAccumulate() {
            machine.selectItem("A1");
            machine.insertMoney(10);
            machine.insertMoney(10);
            machine.insertMoney(10);

            assertEquals(30 - COKE_PRICE, machine.dispense().change());
        }

        @Test
        @DisplayName("dispensing reduces that slot's stock by exactly one")
        void dispenseReducesStock() {
            machine.selectItem("A1");
            machine.insertMoney(COKE_PRICE);
            machine.dispense();

            assertEquals(1, inventory.getSlot("A1").getQuantity());
        }

        @Test
        @DisplayName("the machine is reusable: a second purchase works after the first")
        void machineIsReusable() {
            machine.selectItem("A1");
            machine.insertMoney(COKE_PRICE);
            machine.dispense();

            machine.selectItem("B1");
            machine.insertMoney(WATER_PRICE);

            assertEquals("Water", machine.dispense().item().getName());
        }

        @Test
        @DisplayName("money does not leak between transactions")
        void moneyDoesNotLeakBetweenTransactions() {
            machine.selectItem("A1");
            machine.insertMoney(50);
            machine.dispense();

            // the 25 change from the last purchase must not count towards this one
            machine.selectItem("A1");
            machine.insertMoney(10);

            assertThrows(IllegalStateException.class, () -> machine.dispense());
        }

        @Test
        @DisplayName("draining a slot marks it sold out and blocks further selection")
        void drainingASlotSellsItOut() {
            for (int i = 0; i < 2; i++) {
                machine.selectItem("A1");
                machine.insertMoney(COKE_PRICE);
                machine.dispense();
            }

            assertTrue(inventory.getSlot("A1").isEmpty());
            assertEquals("Item is sold out.",
                    assertThrows(IllegalStateException.class, () -> machine.selectItem("A1")).getMessage());
        }
    }

    @Nested
    @DisplayName("illegal sequences")
    class IllegalSequences {

        @Test
        @DisplayName("dispense with nothing selected is rejected, not an NPE")
        void dispenseWithoutSelection() {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> machine.dispense());

            assertEquals("No item selected.", e.getMessage());
        }

        @Test
        @DisplayName("inserting money with nothing selected is rejected")
        void insertMoneyWithoutSelection() {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> machine.insertMoney(10));

            assertEquals("No item selected.", e.getMessage());
        }

        @Test
        @DisplayName("selecting twice is rejected and the first selection survives")
        void selectTwiceIsRejected() {
            machine.selectItem("A1");

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> machine.selectItem("B1"));
            assertEquals("An item is already selected.", e.getMessage());

            // the original selection must still be the live one
            machine.insertMoney(COKE_PRICE);
            assertEquals("Coke", machine.dispense().item().getName());
        }

        @Test
        @DisplayName("underpaying is rejected and the money is still held")
        void underpayingIsRejected() {
            machine.selectItem("A1");
            machine.insertMoney(10);

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> machine.dispense());
            assertEquals("Insufficient funds. Please insert more money.", e.getMessage());

            // the 10 was not swallowed: topping up completes the purchase
            machine.insertMoney(15);
            assertEquals(0, machine.dispense().change());
        }

        @Test
        @DisplayName("a rejected dispense does not consume stock")
        void rejectedDispenseKeepsStock() {
            machine.selectItem("A1");
            machine.insertMoney(10);

            assertThrows(IllegalStateException.class, () -> machine.dispense());

            assertEquals(2, inventory.getSlot("A1").getQuantity());
        }

        @Test
        @DisplayName("selecting a sold-out slot is rejected")
        void selectSoldOutSlot() {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> machine.selectItem("A2"));

            assertEquals("Item is sold out.", e.getMessage());
        }

        @Test
        @DisplayName("selecting an unknown code is rejected")
        void selectUnknownCode() {
            assertThrows(IllegalArgumentException.class, () -> machine.selectItem("Z9"));
        }

        @Test
        @DisplayName("non-positive amounts are rejected")
        void nonPositiveAmountsRejected() {
            machine.selectItem("A1");

            assertThrows(IllegalArgumentException.class, () -> machine.insertMoney(0));
            assertThrows(IllegalArgumentException.class, () -> machine.insertMoney(-100));
        }

        @Test
        @DisplayName("a negative insert cannot erase money already inserted")
        void negativeInsertCannotEraseBalance() {
            machine.selectItem("A1");
            machine.insertMoney(COKE_PRICE);

            assertThrows(IllegalArgumentException.class, () -> machine.insertMoney(-COKE_PRICE));

            assertEquals(0, machine.dispense().change());
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("refunds everything inserted")
        void refundsEverythingInserted() {
            machine.selectItem("A1");
            machine.insertMoney(10);
            machine.insertMoney(5);

            assertEquals(15, machine.cancel());
        }

        @Test
        @DisplayName("is a no-op on an idle machine, not an error")
        void isNoOpWhenIdle() {
            assertEquals(0, machine.cancel());
        }

        @Test
        @DisplayName("does not restore stock, because dispense had not decremented it")
        void doesNotRestoreStock() {
            machine.selectItem("A1");
            machine.insertMoney(COKE_PRICE);
            machine.cancel();

            assertEquals(2, inventory.getSlot("A1").getQuantity());
        }

        @Test
        @DisplayName("returns the machine to idle so a new purchase can start")
        void returnsMachineToIdle() {
            machine.selectItem("A1");
            machine.insertMoney(10);
            machine.cancel();

            machine.selectItem("B1");
            machine.insertMoney(WATER_PRICE);
            assertEquals("Water", machine.dispense().item().getName());
        }

        @Test
        @DisplayName("refunded money is not credited to the next purchase")
        void refundedMoneyIsNotCarriedOver() {
            machine.selectItem("A1");
            machine.insertMoney(COKE_PRICE);
            machine.cancel();

            machine.selectItem("A1");
            machine.insertMoney(10);

            assertThrows(IllegalStateException.class, () -> machine.dispense());
        }
    }

    @Nested
    @DisplayName("the menu")
    class Menu {

        @Test
        @DisplayName("shows every slot, including sold-out ones, with its code")
        void showsEverySlotWithItsCode() {
            List<ItemView> menu = machine.showItems();

            assertEquals(3, menu.size());
            assertEquals(List.of("A1", "A2", "B1"), menu.stream().map(ItemView::slotCode).toList());
        }

        @Test
        @DisplayName("marks a sold-out slot unavailable but still lists it")
        void marksSoldOutSlotUnavailable() {
            ItemView chips = viewOf("A2");

            assertEquals("Chips", chips.name());
            assertEquals(20, chips.price());
            assertFalse(chips.available());
        }

        @Test
        @DisplayName("availability flips once a slot is drained")
        void availabilityFlipsWhenDrained() {
            assertTrue(viewOf("B1").available());

            machine.selectItem("B1");
            machine.insertMoney(WATER_PRICE);
            machine.dispense();

            assertFalse(viewOf("B1").available());
        }

        @Test
        @DisplayName("is readable in every state")
        void isReadableInEveryState() {
            assertEquals(3, machine.showItems().size());

            machine.selectItem("A1");
            assertEquals(3, machine.showItems().size());

            machine.insertMoney(10);
            assertEquals(3, machine.showItems().size());
        }

        private ItemView viewOf(String slotCode) {
            return machine.showItems().stream()
                    .filter(view -> view.slotCode().equals(slotCode))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
