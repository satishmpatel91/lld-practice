package com.vendingmachine;

/** Read-only snapshot of one slot, for display. Carries facts, not capabilities. */
public record ItemView(String slotCode, String name, int price, boolean available) {

    public static ItemView fromSlot(Slot slot) {
        return new ItemView(
                slot.getCode(),
                slot.getItem().getName(),
                slot.getItem().getPrice(),
                !slot.isEmpty());
    }
}
