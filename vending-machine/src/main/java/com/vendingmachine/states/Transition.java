package com.vendingmachine.states;

/**
 * What the machine should become, plus whatever this caller gets back. Returning
 * both is what stops a state from needing a reference to the machine.
 */
public record Transition<T>(State next, T payload) {

    /** For operations that move the machine but return nothing. */
    public static Transition<Void> to(State next) {
        return new Transition<>(next, null);
    }
}
