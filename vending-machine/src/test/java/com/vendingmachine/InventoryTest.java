package com.vendingmachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryTest {

    private final Inventory inventory = new Inventory();
    private final Slot a1 = new Slot("A1", new Item("Coke", 25), 3);

    @Test
    @DisplayName("resolves a slot by its code")
    void resolvesSlotByCode() {
        inventory.addSlot(a1);

        assertSame(a1, inventory.getSlot("A1"));
    }

    @Test
    @DisplayName("rejects a duplicate slot code instead of overwriting stock")
    void rejectsDuplicateSlotCode() {
        inventory.addSlot(a1);

        assertThrows(IllegalArgumentException.class,
                () -> inventory.addSlot(new Slot("A1", new Item("Pepsi", 30), 5)));

        assertSame(a1, inventory.getSlot("A1"));
    }

    @Test
    @DisplayName("rejects an unknown code rather than returning null")
    void rejectsUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> inventory.getSlot("Z9"));
    }

    @Test
    @DisplayName("lists slots in insertion order, so the menu is stable")
    void listsSlotsInInsertionOrder() {
        inventory.addSlot(a1);
        inventory.addSlot(new Slot("A2", new Item("Chips", 20), 1));
        inventory.addSlot(new Slot("B1", new Item("Water", 15), 1));

        assertEquals(java.util.List.of("A1", "A2", "B1"),
                inventory.allSlots().stream().map(Slot::getCode).toList());
    }

    @Test
    @DisplayName("hands out a collection callers cannot empty")
    void handsOutUnmodifiableCollection() {
        inventory.addSlot(a1);
        Collection<Slot> slots = inventory.allSlots();

        assertThrows(UnsupportedOperationException.class, slots::clear);
    }
}
