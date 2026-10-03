package vending.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendingMachineTest {

    private static final Product COKE = new Product("coke", "Coke", Money.of("25"));
    private static final Product CHIPS = new Product("chips", "Chips", Money.of("20"));

    private Inventory inventory;
    private VendingMachine machine;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.addSlot(new ProductSlot("A1", COKE, 10, 2));
        inventory.addSlot(new ProductSlot("A2", CHIPS, 10, 1));
        inventory.addSlot(new ProductSlot("A3", COKE, 10, 0));
        machine = new VendingMachine(inventory);
    }

    @Test
    void anEmptyMachineHasNoSelectionAndNoBalance() {
        assertTrue(machine.selectedSlot().isEmpty());
        assertEquals(Money.ZERO, machine.insertedAmount());
    }

    @Test
    void availableProductsHidesEmptySlots() {
        assertEquals(2, machine.availableProducts().size());
    }

    @Test
    void insertingMoneyAccumulatesABalance() {
        assertEquals(Money.of("20"), machine.insertMoney(Money.of("20")));
        assertEquals(Money.of("50"), machine.insertMoney(Money.of("30")));
    }

    @Test
    void insertingZeroIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> machine.insertMoney(Money.ZERO));
    }

    @Test
    void selectingAnEmptySlotIsRejected() {
        assertThrows(OutOfStockException.class, () -> machine.selectProduct("A3"));
        assertTrue(machine.selectedSlot().isEmpty(), "a rejected selection must not be recorded");
    }

    @Test
    void selectingAnUnknownSlotIsRejected() {
        assertThrows(InvalidSlotException.class, () -> machine.selectProduct("Z9"));
    }

    @Test
    void theHappyPathDispensesTheProductWithChangeAndClearsTheTransaction() {
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("50"));

        DispenseResult result = machine.dispense();

        assertEquals(COKE, result.product());
        assertEquals(Money.of("25"), result.change());
        assertEquals(1, inventory.slotOf("A1").quantity());
        assertTrue(machine.selectedSlot().isEmpty());
        assertEquals(Money.ZERO, machine.insertedAmount());
    }

    @Test
    void exactMoneyGivesNoChange() {
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("25"));

        assertEquals(Money.ZERO, machine.dispense().change());
    }

    @Test
    void dispensingWithoutSelectingIsRejected() {
        machine.insertMoney(Money.of("50"));

        assertThrows(NoProductSelectedException.class, () -> machine.dispense());
    }

    @Test
    void dispensingWithTooLittleMoneyIsRejectedAndReportsTheShortfall() {
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("10"));

        InsufficientBalanceException thrown =
                assertThrows(InsufficientBalanceException.class, () -> machine.dispense());

        assertEquals(Money.of("15"), thrown.shortfall());
    }

    @Test
    void aRejectedDispenseKeepsTheMoneyAndTheSelectionSoTheCustomerCanTopUp() {
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("10"));
        assertThrows(InsufficientBalanceException.class, () -> machine.dispense());

        assertEquals(Money.of("10"), machine.insertedAmount());
        assertTrue(machine.selectedSlot().isPresent());

        machine.insertMoney(Money.of("15"));

        assertEquals(COKE, machine.dispense().product());
    }

    @Test
    void aRejectedDispenseDoesNotTouchStock() {
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("10"));

        assertThrows(InsufficientBalanceException.class, () -> machine.dispense());

        assertEquals(2, inventory.slotOf("A1").quantity());
    }

    @Test
    void cancellingRefundsEverythingAndForgetsTheSelection() {
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("10"));

        assertEquals(Money.of("10"), machine.cancel());
        assertTrue(machine.selectedSlot().isEmpty());
        assertEquals(Money.ZERO, machine.insertedAmount());
    }

    @Test
    void cancellingWithNothingInsertedRefundsZero() {
        assertEquals(Money.ZERO, machine.cancel());
    }

    @Test
    void aSlotEmptiedAfterSelectionIsCaughtAtDispenseTime() {
        machine.selectProduct("A2");
        machine.insertMoney(Money.of("20"));
        inventory.slotOf("A2").dispenseOne();   // someone else took the last one

        assertThrows(OutOfStockException.class, () -> machine.dispense());
        assertEquals(Money.of("20"), machine.insertedAmount(),
                "the customer's money must survive so cancel() can refund it");
    }

    @Test
    void outOfStockBeatsInsufficientBalanceWhenBothAreTrue() {
        machine.selectProduct("A2");
        machine.insertMoney(Money.of("5"));         // not enough for Chips at 20
        inventory.slotOf("A2").dispenseOne();       // and the slot is now empty

        assertThrows(OutOfStockException.class, () -> machine.dispense(),
                "telling the customer to pay more for a product that no longer exists is useless");
    }

    @Test
    void changingTheSelectionKeepsTheMoneyAlreadyInserted() {
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("25"));

        machine.selectProduct("A2");

        assertEquals(Money.of("25"), machine.insertedAmount());
        assertEquals(Money.of("5"), machine.dispense().change());
    }

    @Test
    void buyingTheLastUnitLeavesTheSlotUnavailable() {
        machine.selectProduct("A2");
        machine.insertMoney(Money.of("20"));

        machine.dispense();

        assertEquals(1, machine.availableProducts().size());
        assertThrows(OutOfStockException.class, () -> machine.selectProduct("A2"));
    }
}
