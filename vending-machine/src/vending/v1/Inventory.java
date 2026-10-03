package vending.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the slots of one machine and answers "what is where, and is it available".
 *
 * Knows nothing about money, selection or transactions. That separation is why
 * we can later make stock thread-safe (V4) without touching payment code.
 */
public class Inventory {

    private final Map<String, ProductSlot> slots = new LinkedHashMap<>();

    public void addSlot(ProductSlot slot) {
        if (slots.containsKey(slot.code())) {
            throw new IllegalArgumentException("Slot already exists: " + slot.code());
        }
        slots.put(slot.code(), slot);
    }

    public ProductSlot slotOf(String code) {
        ProductSlot slot = slots.get(code);
        if (slot == null) {
            throw new InvalidSlotException(code);
        }
        return slot;
    }

    /** Only slots a customer can actually buy from. */
    public List<ProductSlot> availableSlots() {
        List<ProductSlot> available = new ArrayList<>();
        for (ProductSlot slot : slots.values()) {
            if (slot.hasStock()) {
                available.add(slot);
            }
        }
        return available;
    }

    /** Everything, including empty slots (useful for refill / admin view). */
    public List<ProductSlot> allSlots() {
        return new ArrayList<>(slots.values());
    }
}
