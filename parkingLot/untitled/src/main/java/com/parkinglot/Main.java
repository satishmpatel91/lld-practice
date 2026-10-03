package com.parkinglot;

import com.parkinglot.constants.GateType;
import com.parkinglot.constants.RatePlan;
import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;
import com.parkinglot.repo.TicketRepository;
import com.parkinglot.service.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {

    private static final Clock CLOCK = Clock.systemUTC();

    public static void main(String[] args) {
        Map<RatePlan, FeeCalculator> calculators = Map.of(
                RatePlan.HOURLY, new HourlyFeeCalculator(new BigDecimal("30")),
                RatePlan.DAILY,  new DailyFeeCalculator(new BigDecimal("500"))
        );
        Gate entryGate = new Gate("G1", GateType.ENTRY);
        Gate exitGate = new Gate("G2", GateType.EXIT);
        ParkingFloor groundFloor = new ParkingFloor("F1", List.of(
                new SpotSpec(SpotType.LARGE, 30),
                new SpotSpec(SpotType.MEDIUM, 10)));
        ParkingFloor firstFloor = new ParkingFloor("F2", List.of(
                new SpotSpec(SpotType.MEDIUM, 50),
                new SpotSpec(SpotType.SMALL, 5)));

        ParkingLot parkingLot = new ParkingLot("LOT-1",
                List.of(groundFloor, firstFloor),
                new NearestToEntranceStrategy());



        TicketRepository ticketRepository = new TicketRepository();

        Vehicle vehicle = new Vehicle("KA-01-HH-1234", VehicleType.SMALL);

        ParkingLotService parkingLotService = new ParkingLotService(parkingLot, CLOCK, ticketRepository, calculators);

        System.out.println("Lot " + parkingLot.getLotNumber() + " spots free for "
                + vehicle.vehicleType() + ": " + parkingLot.freeSpotCount(vehicle.vehicleType()));
        Optional<Ticket> ticketOptional = parkingLotService.park(vehicle, entryGate, RatePlan.HOURLY);
        if(ticketOptional.isEmpty()) {
            System.out.println("No spot available for a " + vehicle.vehicleType() + " vehicle");
            return;
        }

        Ticket ticket = ticketOptional.get();
        System.out.println("Issued ticket " + ticket.ticketId() + " for spot " + ticket.spotNumber());
        System.out.println("Spots free for " + vehicle.vehicleType() + " now: "
                + parkingLot.freeSpotCount(vehicle.vehicleType()));


        Optional<Receipt> receiptOptional = parkingLotService.unpark(ticket.ticketId(), exitGate);
        if (receiptOptional.isEmpty()) {
            System.out.println("Ticket " + ticket.ticketId() + " is not valid");
            return;
        }

        Receipt receipt = receiptOptional.get();
        System.out.println("Receipt " + receipt.receiptId() + " issued at gate " + receipt.gateNo()
                + " for ticket " + receipt.ticket().ticketId() + " with price " + receipt.price());
        System.out.println("Spots free for " + vehicle.vehicleType() + " now: "
                + parkingLot.freeSpotCount(vehicle.vehicleType()));
    }
}
