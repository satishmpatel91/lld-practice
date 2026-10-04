package com.vendingmachine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.vendingmachine.Operation.*;

/**
 * Where the machine is in a purchase, and which operations each state permits.
 * The transition table lives here, one line per state, so "what is legal in
 * ITEM_SELECTED?" is answered by reading one line instead of grepping guards.
 *
 * Only rules decidable from the state alone belong here. Data conditions
 * ("sold out", "insufficient funds") stay in the methods that own the data.
 */
enum MachineState {

    IDLE(EnumSet.of(SHOW_ITEMS, SELECT_ITEM, CANCEL)),

    ITEM_SELECTED(EnumSet.of(SHOW_ITEMS, INSERT_MONEY, DISPENSE, CANCEL));

    private final Set<Operation> allowedOperations;

    MachineState(EnumSet<Operation> allowedOperations) {
        this.allowedOperations = Collections.unmodifiableSet(EnumSet.copyOf(allowedOperations));
    }

    public boolean isOperationAllowed(Operation operation) {
        return allowedOperations.contains(operation);
    }

    public void requireOperation(Operation operation, String message) {
        if (!isOperationAllowed(operation)) {
            throw new IllegalStateException(message);
        }
    }
}
