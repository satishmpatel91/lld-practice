package com.parkinglot.service;

import com.parkinglot.Ticket;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public class HourlyFeeCalculator implements FeeCalculator {

    private final BigDecimal ratePerHour;

    public HourlyFeeCalculator(BigDecimal ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    @Override
    public BigDecimal calculate(Ticket ticket, Instant exitTime) {
        long minutesParked = Duration.between(ticket.entryTime(), exitTime).toMinutes();
        long chargeableHours = Math.max(1, (long) Math.ceil(minutesParked / 60.0));
        return ratePerHour.multiply(BigDecimal.valueOf(chargeableHours));
    }
}
