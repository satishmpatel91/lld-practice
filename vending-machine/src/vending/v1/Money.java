package vending.v1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object for an amount of money.
 *
 * Why it exists: price, inserted balance and change are all "amounts of money".
 * Passing them around as double/int invites currency bugs and silent rounding
 * errors. Making it a type gives us one place to enforce the rules:
 * never negative, always 2 decimal places, compared by value not identity.
 *
 * Immutable on purpose: a balance is never mutated, it is replaced.
 */
public final class Money implements Comparable<Money> {

    public static final Money ZERO = Money.of("0");

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + amount);
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /** Caller must check isLessThan() first; failing fast beats a negative balance. */
    public Money subtract(Money other) {
        if (this.isLessThan(other)) {
            throw new IllegalArgumentException("Cannot subtract " + other + " from " + this);
        }
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public BigDecimal amount() {
        return amount;
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        return amount.compareTo(((Money) o).amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "Rs." + amount;
    }
}
