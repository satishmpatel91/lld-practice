package com.parkinglot;

import java.math.BigDecimal;
import java.time.Instant;

public record Receipt(
        String receiptId,
        String gateNo,
        Ticket ticket,
        Instant exitTime,
        BigDecimal price
) {
}
