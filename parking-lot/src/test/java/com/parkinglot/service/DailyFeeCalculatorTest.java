package com.parkinglot.service;

import com.parkinglot.Ticket;
import com.parkinglot.constants.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyFeeCalculatorTest {

    private static final Instant ENTRY = Instant.parse("2026-10-03T10:00:00Z");
    private static final BigDecimal RATE_PER_DAY = new BigDecimal("500");

    private final FeeCalculator calculator = new DailyFeeCalculator(RATE_PER_DAY);

    @Test
    void aShortStayIsChargedOneDay() {
        assertPrice("500", Duration.ofMinutes(10));
    }

    @Test
    void justUnderADayIsChargedOneDay() {
        assertPrice("500", Duration.ofHours(23));
    }

    @Test
    void exactlyOneDayIsChargedOneDay() {
        assertPrice("500", Duration.ofHours(24));
    }

    @Test
    void anHourIntoTheSecondDayIsChargedTwoDays() {
        assertPrice("1000", Duration.ofHours(25));
    }

    @Test
    void almostTwoDaysIsChargedTwoDays() {
        assertPrice("1000", Duration.ofHours(47));
    }

    @Test
    void exactlyTwoDaysIsChargedTwoDays() {
        assertPrice("1000", Duration.ofHours(48));
    }

    @Test
    void aMinuteIntoTheThirdDayIsChargedThreeDays() {
        assertPrice("1500", Duration.ofHours(48).plusMinutes(1));
    }

    @Test
    void aZeroLengthStayStillCostsTheMinimumOneDay() {
        assertPrice("500", Duration.ZERO);
    }

    @Test
    void theInjectedRateIsWhatGetsCharged() {
        FeeCalculator pricier = new DailyFeeCalculator(new BigDecimal("800"));

        BigDecimal price = pricier.calculate(ticket(), ENTRY.plus(Duration.ofHours(25)));

        assertEquals(0, price.compareTo(new BigDecimal("1600")),
                () -> "expected 1600 but was " + price);
    }

    private void assertPrice(String expected, Duration stay) {
        BigDecimal price = calculator.calculate(ticket(), ENTRY.plus(stay));

        assertEquals(0, price.compareTo(new BigDecimal(expected)),
                () -> "stay of " + stay.toHours() + "h: expected " + expected + " but was " + price);
    }

    private Ticket ticket() {
        return new Ticket("T-1", "G1", "F1-S1", "KA-01-HH-1234", ENTRY, RatePlan.DAILY);
    }
}
