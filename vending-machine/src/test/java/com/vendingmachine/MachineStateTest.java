package com.vendingmachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.vendingmachine.MachineState.IDLE;
import static com.vendingmachine.MachineState.ITEM_SELECTED;
import static com.vendingmachine.Operation.CANCEL;
import static com.vendingmachine.Operation.DISPENSE;
import static com.vendingmachine.Operation.INSERT_MONEY;
import static com.vendingmachine.Operation.SELECT_ITEM;
import static com.vendingmachine.Operation.SHOW_ITEMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The transition table, asserted directly rather than through the machine. */
class MachineStateTest {

    @Test
    @DisplayName("IDLE permits looking, selecting and cancelling")
    void idlePermits() {
        assertTrue(IDLE.isOperationAllowed(SHOW_ITEMS));
        assertTrue(IDLE.isOperationAllowed(SELECT_ITEM));
        assertTrue(IDLE.isOperationAllowed(CANCEL));
    }

    @Test
    @DisplayName("IDLE forbids paying and dispensing")
    void idleForbids() {
        assertFalse(IDLE.isOperationAllowed(INSERT_MONEY));
        assertFalse(IDLE.isOperationAllowed(DISPENSE));
    }

    @Test
    @DisplayName("ITEM_SELECTED permits paying, dispensing, cancelling and looking")
    void itemSelectedPermits() {
        assertTrue(ITEM_SELECTED.isOperationAllowed(INSERT_MONEY));
        assertTrue(ITEM_SELECTED.isOperationAllowed(DISPENSE));
        assertTrue(ITEM_SELECTED.isOperationAllowed(CANCEL));
        assertTrue(ITEM_SELECTED.isOperationAllowed(SHOW_ITEMS));
    }

    @Test
    @DisplayName("ITEM_SELECTED forbids selecting a second item")
    void itemSelectedForbidsSecondSelection() {
        assertFalse(ITEM_SELECTED.isOperationAllowed(SELECT_ITEM));
    }

    @Test
    @DisplayName("requireOperation throws the caller's message when forbidden")
    void requireOperationThrowsCallersMessage() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> IDLE.requireOperation(DISPENSE, "No item selected."));

        assertEquals("No item selected.", e.getMessage());
    }

    @Test
    @DisplayName("requireOperation is silent when the operation is permitted")
    void requireOperationSilentWhenPermitted() {
        IDLE.requireOperation(SELECT_ITEM, "should not throw");
    }

    @Test
    @DisplayName("every operation is permitted by at least one state")
    void everyOperationIsReachable() {
        for (Operation operation : Operation.values()) {
            assertTrue(IDLE.isOperationAllowed(operation) || ITEM_SELECTED.isOperationAllowed(operation),
                    operation + " is permitted by no state, so no code path can ever run it");
        }
    }
}
