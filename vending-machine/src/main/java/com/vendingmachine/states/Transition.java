package com.vendingmachine.states;

/** What the machine should become, plus whatever the caller gets back. */
public record Transition<T>(State next, T payload) {

    /** For operations that change state but return nothing to the caller. */
    public static Transition<Void> to(State next) {
        return new Transition<>(next, null);
    }
}
