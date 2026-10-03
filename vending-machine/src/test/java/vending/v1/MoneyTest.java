package vending.v1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void anAmountIsNormalisedToTwoDecimalPlaces() {
        assertEquals("25.00", Money.of("25").amount().toString());
        assertEquals("25.50", Money.of("25.5").amount().toString());
    }

    @Test
    void amountsWithDifferentScalesButTheSameValueAreEqual() {
        assertEquals(Money.of("25"), Money.of("25.00"));
        assertEquals(Money.of("25").hashCode(), Money.of("25.00").hashCode(),
                "equal values must hash the same, or Money breaks inside a HashMap");
    }

    @Test
    void aNegativeAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("-1"));
    }

    @Test
    void addingProducesANewAmountAndLeavesBothOperandsAlone() {
        Money twenty = Money.of("20");
        Money thirty = Money.of("30");

        assertEquals(Money.of("50"), twenty.add(thirty));
        assertEquals(Money.of("20"), twenty);
        assertEquals(Money.of("30"), thirty);
    }

    @Test
    void subtractingYieldsTheDifference() {
        assertEquals(Money.of("25"), Money.of("50").subtract(Money.of("25")));
    }

    @Test
    void subtractingMoreThanYouHaveIsRejectedRatherThanGoingNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of("10").subtract(Money.of("25")));
    }

    @Test
    void subtractingAnEqualAmountIsAllowedAndGivesZero() {
        assertEquals(Money.ZERO, Money.of("25").subtract(Money.of("25")));
        assertTrue(Money.of("25").subtract(Money.of("25")).isZero());
    }

    @Test
    void comparisonIsByValue() {
        assertTrue(Money.of("10").isLessThan(Money.of("25")));
        assertFalse(Money.of("25").isLessThan(Money.of("25")));
        assertFalse(Money.of("30").isLessThan(Money.of("25")));
    }

    @Test
    void zeroIsZero() {
        assertTrue(Money.ZERO.isZero());
        assertFalse(Money.of("0.01").isZero());
    }

    @Test
    void roundingUsesHalfUpAtTheSecondDecimalPlace() {
        assertEquals(Money.of("1.24"), Money.of("1.235").subtract(Money.ZERO).subtract(Money.ZERO));
        assertEquals("1.24", Money.of("1.235").amount().toString());
        assertEquals("1.23", Money.of("1.234").amount().toString());
    }
}
