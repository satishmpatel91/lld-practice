package com.vendingmachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotTest {

    @Test
    @DisplayName("a stocked slot is not empty")
    void stockedSlotIsNotEmpty() {
        assertFalse(new Slot("A1", new Item("Coke", 25), 1).isEmpty());
    }

    @Test
    @DisplayName("a slot built with zero quantity is already empty")
    void zeroQuantitySlotIsEmpty() {
        assertTrue(new Slot("A2", new Item("Chips", 20), 0).isEmpty());
    }

    @Test
    @DisplayName("reducing stock takes exactly one unit")
    void reducingTakesOneUnit() {
        Slot slot = new Slot("A1", new Item("Coke", 25), 2);

        slot.reduceQuantity();

        assertEquals(1, slot.getQuantity());
    }

    @Test
    @DisplayName("an empty slot refuses to go negative")
    void emptySlotRefusesToGoNegative() {
        Slot slot = new Slot("A1", new Item("Coke", 25), 1);
        slot.reduceQuantity();

        assertThrows(IllegalStateException.class, slot::reduceQuantity);
        assertEquals(0, slot.getQuantity());
    }

    @Test
    @DisplayName("a view maps the slot's code, item and availability")
    void viewMapsSlotFacts() {
        ItemView view = ItemView.fromSlot(new Slot("B1", new Item("Water", 15), 2));

        assertEquals("B1", view.slotCode());
        assertEquals("Water", view.name());
        assertEquals(15, view.price());
        assertTrue(view.available());
    }
}
