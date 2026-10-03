package vending.v1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSlotTest {

    private static final Product COKE = new Product("coke", "Coke", Money.of("25"));

    @Test
    void aNewSlotReportsItsStock() {
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 2);

        assertEquals("A1", slot.code());
        assertEquals(COKE, slot.product());
        assertEquals(10, slot.capacity());
        assertEquals(2, slot.quantity());
        assertTrue(slot.hasStock());
    }

    @Test
    void anEmptySlotHasNoStock() {
        assertFalse(new ProductSlot("A1", COKE, 10, 0).hasStock());
    }

    @Test
    void dispensingDecrementsTheCountByOne() {
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 2);

        slot.dispenseOne();

        assertEquals(1, slot.quantity());
    }

    @Test
    void dispensingFromAnEmptySlotIsRejected() {
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 1);
        slot.dispenseOne();

        OutOfStockException thrown = assertThrows(OutOfStockException.class, slot::dispenseOne);

        assertTrue(thrown.getMessage().contains("A1"));
        assertEquals(0, slot.quantity(), "a rejected dispense must not change the count");
    }

    @Test
    void refillingAddsStock() {
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 2);

        slot.refill(5);

        assertEquals(7, slot.quantity());
    }

    @Test
    void refillingBeyondCapacityIsRejected() {
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 8);

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> slot.refill(3));

        assertTrue(thrown.getMessage().contains("2 more"), thrown.getMessage());
        assertEquals(8, slot.quantity(), "a rejected refill must not change the count");
    }

    @Test
    void refillingToExactlyCapacityIsAllowed() {
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 8);

        slot.refill(2);

        assertEquals(10, slot.quantity());
    }

    @Test
    void aNonPositiveRefillIsRejected() {
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 2);

        assertThrows(IllegalArgumentException.class, () -> slot.refill(0));
        assertThrows(IllegalArgumentException.class, () -> slot.refill(-1));
    }

    @Test
    void aSlotCannotBeBuiltWithNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ProductSlot("A1", COKE, 0, 0));
    }

    @Test
    void aSlotCannotBeBuiltOverCapacityOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> new ProductSlot("A1", COKE, 5, 6));
        assertThrows(IllegalArgumentException.class, () -> new ProductSlot("A1", COKE, 5, -1));
    }
}
