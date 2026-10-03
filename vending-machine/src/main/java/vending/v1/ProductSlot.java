package vending.v1;

import java.util.Objects;

/**
 * One physical slot ("A1") holding N units of one product.
 *
 * This is the ONLY class allowed to change a stock count. Nobody outside can
 * do `slot.quantity = slot.quantity - 1`; they must ask the slot to dispense.
 * That single rule is what makes stock correctness reviewable in one place
 * (and is what we will lock in V4).
 */
public class ProductSlot {

    private final String code;
    private final Product product;
    private final int capacity;
    private int quantity;

    public ProductSlot(String code, Product product, int capacity, int quantity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (quantity < 0 || quantity > capacity) {
            throw new IllegalArgumentException("quantity must be within [0, capacity]");
        }
        this.code = Objects.requireNonNull(code, "code");
        this.product = Objects.requireNonNull(product, "product");
        this.capacity = capacity;
        this.quantity = quantity;
    }

    public boolean hasStock() {
        return quantity > 0;
    }

    /** Tell-don't-ask: the slot decrements itself, callers never touch the count. */
    public void dispenseOne() {
        if (!hasStock()) {
            throw new OutOfStockException(code, product.name());
        }
        quantity--;
    }

    public void refill(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("refill count must be > 0");
        }
        if (quantity + count > capacity) {
            throw new IllegalArgumentException(
                    "Slot " + code + " can hold " + (capacity - quantity) + " more unit(s)");
        }
        quantity += count;
    }

    public String code()     { return code; }
    public Product product() { return product; }
    public int quantity()    { return quantity; }
    public int capacity()    { return capacity; }

    @Override
    public String toString() {
        return code + " -> " + product + " x" + quantity;
    }
}
