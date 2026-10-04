package com.vendingmachine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Owns every slot in the machine and resolves a slot by its code. */
public class Inventory {
    private final Map<String, Slot> slots = new LinkedHashMap<>();

    public void addSlot(Slot slot) {
        Slot existing = slots.putIfAbsent(slot.getCode(), slot);
        if (existing != null) {
            throw new IllegalArgumentException("Slot with code " + slot.getCode() + " already exists.");
        }
    }

    public Slot getSlot(String code) {
        Slot slot = slots.get(code);
        if (slot == null) {
            throw new IllegalArgumentException("Slot with code " + code + " does not exist.");
        }
        return slot;
    }

    public Collection<Slot> allSlots() {
        return Collections.unmodifiableCollection(slots.values());
    }
}
