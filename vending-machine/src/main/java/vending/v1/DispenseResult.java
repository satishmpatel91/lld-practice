package vending.v1;

import java.util.Objects;

/**
 * What the customer physically receives: the product and the change.
 *
 * Returning one object avoids an API where change has to be fetched by a second
 * call ("dispense(); then getChange();") - two calls means two chances to forget
 * one, and a window where the machine is in a half-finished state.
 */
public final class DispenseResult {

    private final Product product;
    private final Money change;

    public DispenseResult(Product product, Money change) {
        this.product = Objects.requireNonNull(product);
        this.change = Objects.requireNonNull(change);
    }

    public Product product() { return product; }
    public Money change()    { return change; }

    @Override
    public String toString() {
        return "Dispensed " + product.name() + ", change " + change;
    }
}
