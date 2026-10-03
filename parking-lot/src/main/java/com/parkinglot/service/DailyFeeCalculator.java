package com.parkinglot.service;

import com.parkinglot.Ticket;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public class DailyFeeCalculator implements FeeCalculator {

    private final BigDecimal ratePerDay;
    private static final long MINUTES_PER_DAY = 24 * 60;

    public DailyFeeCalculator(BigDecimal ratePerDay) {
        this.ratePerDay = ratePerDay;
    }

    @Override
    public BigDecimal calculate(Ticket ticket, Instant exitTime) {
        long minutesParked = Duration.between(ticket.entryTime(), exitTime).toMinutes();
        long chargeableDays = Math.max(1, (long) Math.ceil((double) minutesParked / MINUTES_PER_DAY));
        return ratePerDay.multiply(BigDecimal.valueOf(chargeableDays));
    }
}
