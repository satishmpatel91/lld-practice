package com.parkinglot;

import com.parkinglot.constants.RatePlan;

import java.time.Instant;

public record Ticket(
        String ticketId,
        String gateNo,
        String spotNumber,
        String vehicleNumber,
        Instant entryTime,
        RatePlan ratePlan
) {
}
