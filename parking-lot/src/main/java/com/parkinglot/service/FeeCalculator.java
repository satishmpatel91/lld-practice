package com.parkinglot.service;

import com.parkinglot.Ticket;

import java.math.BigDecimal;
import java.time.Instant;

public interface FeeCalculator {
    BigDecimal calculate(Ticket ticket, Instant exitTime);
}
