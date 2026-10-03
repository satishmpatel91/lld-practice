package vending.v1;

import java.util.Objects;

/**
 * What is being sold. Pure data + its price.
 *
 * Deliberately does NOT know how many of it are left. Stock is a property of a
 * physical slot in a machine, not of the product itself: the same Coke exists
 * in 50 machines with 50 different counts.
 */
public final class Product {

    private final String id;
    private final String name;
    private final Money price;

    public Product(String id, String name, Money price) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.price = Objects.requireNonNull(price, "price");
    }

    public String id()    { return id; }
    public String name()  { return name; }
    public Money price()  { return price; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        return id.equals(((Product) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return name + " (" + price + ")"; }
}
