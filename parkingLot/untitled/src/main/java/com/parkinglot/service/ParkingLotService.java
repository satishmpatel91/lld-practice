package com.parkinglot.service;

import com.parkinglot.Gate;
import com.parkinglot.ParkingLot;
import com.parkinglot.ParkingSpot;
import com.parkinglot.Receipt;
import com.parkinglot.Ticket;
import com.parkinglot.Vehicle;
import com.parkinglot.constants.GateType;
import com.parkinglot.constants.RatePlan;
import com.parkinglot.repo.TicketRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;


public class ParkingLotService {

    private final ParkingLot parkingLot;
    private final Clock clock;
    private final TicketRepository ticketRepository;

    private final Map<RatePlan, FeeCalculator> feeCalculators;

    public ParkingLotService(ParkingLot parkingLot,
                             Clock clock,
                             TicketRepository ticketRepository,
                             Map<RatePlan, FeeCalculator> feeCalculators) {
        Objects.requireNonNull(feeCalculators, "feeCalculators");

        EnumSet<RatePlan> missing = EnumSet.allOf(RatePlan.class);
        missing.removeAll(feeCalculators.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("No fee calculator configured for rate plans: " + missing);
        }
        this.parkingLot = parkingLot;
        this.clock = clock;
        this.ticketRepository = ticketRepository;
        this.feeCalculators = Map.copyOf(feeCalculators);
    }

    public Optional<Ticket> park(Vehicle vehicle, Gate gate, RatePlan ratePlan) {
        requireGateType(gate, GateType.ENTRY);
        Optional<ParkingSpot> freeSpot = parkingLot.claimFreeSpot(vehicle.vehicleType());
        if(freeSpot.isEmpty()) {
            return Optional.empty();
        }

        ParkingSpot parkingSpot = freeSpot.get();
        Ticket ticket = new Ticket(
                UUID.randomUUID().toString(),
                gate.getGateId(),
                parkingSpot.getSpotNumber(),
                vehicle.vehicleNumber(),
                Instant.now(clock),
                ratePlan
        );
        ticketRepository.save(ticket);
        return Optional.of(ticket);
    }

    public Optional<Receipt> unpark(String ticketId, Gate gate) {
        requireGateType(gate, GateType.EXIT);
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
        if (optionalTicket.isEmpty()) {
            return Optional.empty();
        }

        Ticket ticket = optionalTicket.get();

        Instant exitTime = Instant.now(clock);

        BigDecimal price = feeCalculators.get(ticket.ratePlan()).calculate(ticket, exitTime);
        Receipt receipt = new Receipt(
                UUID.randomUUID().toString(),
                gate.getGateId(),
                ticket,
                exitTime,
                price
        );

        parkingLot.findSpot(ticket.spotNumber())
                .orElseThrow(() -> new IllegalStateException("Unknown spot " + ticket.spotNumber()))
                .free();
        ticketRepository.remove(ticketId);
        return Optional.of(receipt);
    }


    private void requireGateType(Gate gate, GateType expected) {

        if(gate.getGateType() != expected) {
            throw new IllegalArgumentException("Gate " + gate.getGateId() + " is of type " + gate.getGateType() + ", but " + expected + " was expected");
        }
    }
}