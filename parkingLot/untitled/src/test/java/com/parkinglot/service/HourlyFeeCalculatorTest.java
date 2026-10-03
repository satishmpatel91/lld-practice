package com.parkinglot.service;

import com.parkinglot.Ticket;
import com.parkinglot.constants.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HourlyFeeCalculatorTest {

    private static final Instant ENTRY = Instant.parse("2026-10-03T10:00:00Z");
    private static final BigDecimal RATE_PER_HOUR = new BigDecimal("30");

    private final FeeCalculator calculator = new HourlyFeeCalculator(RATE_PER_HOUR);

    @Test
    void aStayUnderAnHourIsChargedOneHour() {
        assertPrice("30", Duration.ofMinutes(10));
    }

    @Test
    void anExactHourIsChargedOneHour() {
        assertPrice("30", Duration.ofMinutes(60));
    }

    @Test
    void aSingleMinuteIntoTheSecondHourIsChargedTwoHours() {
        assertPrice("60", Duration.ofMinutes(61));
    }

    @Test
    void almostTwoHoursIsChargedTwoHours() {
        assertPrice("60", Duration.ofMinutes(119));
    }

    @Test
    void exactlyTwoHoursIsChargedTwoHours() {
        assertPrice("60", Duration.ofMinutes(120));
    }

    @Test
    void aLongStayIsChargedPerRoundedUpHour() {
        assertPrice("90", Duration.ofMinutes(179));
    }

    @Test
    void aZeroLengthStayStillCostsTheMinimumOneHour() {
        assertPrice("30", Duration.ZERO);
    }

    @Test
    void theInjectedRateIsWhatGetsCharged() {
        FeeCalculator cheaper = new HourlyFeeCalculator(new BigDecimal("10"));

        BigDecimal price = cheaper.calculate(ticket(), ENTRY.plus(Duration.ofMinutes(61)));

        assertEquals(0, price.compareTo(new BigDecimal("20")),
                () -> "expected 20 but was " + price);
    }

    private void assertPrice(String expected, Duration stay) {
        BigDecimal price = calculator.calculate(ticket(), ENTRY.plus(stay));

        assertEquals(0, price.compareTo(new BigDecimal(expected)),
                () -> "stay of " + stay.toMinutes() + "m: expected " + expected + " but was " + price);
    }

    private Ticket ticket() {
        return new Ticket("T-1", "G1", "F1-S1", "KA-01-HH-1234", ENTRY, RatePlan.HOURLY);
    }
}
