package vending.v1;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTest {

    private static final Product COKE = new Product("coke", "Coke", Money.of("25"));
    private static final Product CHIPS = new Product("chips", "Chips", Money.of("20"));

    @Test
    void aSlotCanBeLookedUpByItsCode() {
        Inventory inventory = new Inventory();
        ProductSlot slot = new ProductSlot("A1", COKE, 10, 2);
        inventory.addSlot(slot);

        assertSame(slot, inventory.slotOf("A1"));
    }

    @Test
    void anUnknownSlotCodeIsRejected() {
        Inventory inventory = new Inventory();

        InvalidSlotException thrown =
                assertThrows(InvalidSlotException.class, () -> inventory.slotOf("Z9"));

        assertTrue(thrown.getMessage().contains("Z9"));
    }

    @Test
    void aDuplicateSlotCodeIsRejected() {
        Inventory inventory = new Inventory();
        inventory.addSlot(new ProductSlot("A1", COKE, 10, 2));

        assertThrows(IllegalArgumentException.class,
                () -> inventory.addSlot(new ProductSlot("A1", CHIPS, 10, 2)));
    }

    @Test
    void availableSlotsExcludesEmptyOnes() {
        Inventory inventory = new Inventory();
        inventory.addSlot(new ProductSlot("A1", COKE, 10, 2));
        inventory.addSlot(new ProductSlot("A2", CHIPS, 10, 0));

        assertEquals(List.of("A1"), codesOf(inventory.availableSlots()));
        assertEquals(List.of("A1", "A2"), codesOf(inventory.allSlots()));
    }

    @Test
    void slotsKeepInsertionOrder() {
        Inventory inventory = new Inventory();
        inventory.addSlot(new ProductSlot("B2", COKE, 10, 1));
        inventory.addSlot(new ProductSlot("A1", CHIPS, 10, 1));

        assertEquals(List.of("B2", "A1"), codesOf(inventory.allSlots()),
                "display order is the order slots were added, not alphabetical");
    }

    @Test
    void theReturnedListsAreCopiesSoCallersCannotChangeTheInventory() {
        Inventory inventory = new Inventory();
        inventory.addSlot(new ProductSlot("A1", COKE, 10, 1));

        inventory.allSlots().clear();
        inventory.availableSlots().clear();

        assertEquals(1, inventory.allSlots().size());
    }

    private List<String> codesOf(List<ProductSlot> slots) {
        return slots.stream().map(ProductSlot::code).toList();
    }
}
