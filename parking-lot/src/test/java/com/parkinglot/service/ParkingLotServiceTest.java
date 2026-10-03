package com.parkinglot.service;

import com.parkinglot.ParkingLot;
import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;
import com.parkinglot.ParkingFloor;
import com.parkinglot.SpotSpec;
import com.parkinglot.Receipt;
import com.parkinglot.Ticket;
import com.parkinglot.Vehicle;
import com.parkinglot.Gate;
import com.parkinglot.constants.GateType;
import com.parkinglot.constants.RatePlan;
import com.parkinglot.repo.TicketRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingLotServiceTest {

    private static final Instant START = Instant.parse("2026-10-02T10:00:00Z");
    private static final BigDecimal RATE_PER_HOUR = new BigDecimal("30");
    private static final Vehicle VEHICLE = new Vehicle("KA-01-HH-1234", VehicleType.MEDIUM);
    private static final BigDecimal RATE_PER_DAY = new BigDecimal("500");
    private static final Map<RatePlan, FeeCalculator> CALCULATORS = Map.of(
            RatePlan.HOURLY, new HourlyFeeCalculator(RATE_PER_HOUR),
            RatePlan.DAILY, new DailyFeeCalculator(RATE_PER_DAY));

    private static final Gate ENTRY_GATE = new Gate("G1", GateType.ENTRY);
    private static final Gate EXIT_GATE = new Gate("G2", GateType.EXIT);

    private final AdvanceableClock clock = new AdvanceableClock(START);
    private final TicketRepository ticketRepository = new TicketRepository();

    @Test
    void aCalculatorMapMissingARatePlanIsRejectedAtConstruction() {
        ParkingLot lot = lotOf(SpotType.MEDIUM);
        Map<RatePlan, FeeCalculator> onlyHourly =
                Map.of(RatePlan.HOURLY, new HourlyFeeCalculator(RATE_PER_HOUR));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new ParkingLotService(lot, clock, ticketRepository, onlyHourly));

        assertTrue(thrown.getMessage().contains("DAILY"),
                () -> "the message should name the missing plan, but was: " + thrown.getMessage());
    }

    @Test
    void parkIssuesATicketForTheAssignedSpot() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM, SpotType.MEDIUM));

        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        assertEquals("F1-S1", ticket.spotNumber());
        assertEquals("KA-01-HH-1234", ticket.vehicleNumber());
        assertFalse(ticket.ticketId().isBlank());
    }

    @Test
    void entryTimeComesFromTheInjectedClock() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));

        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        assertEquals(START, ticket.entryTime());
    }

    @Test
    void parkOccupiesTheAssignedSpot() {
        ParkingLot lot = lotOf(SpotType.MEDIUM, SpotType.MEDIUM);
        ParkingLotService service = serviceFor(lot);

        service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        assertEquals(1, lot.freeSpotCount(VehicleType.MEDIUM));
        assertTrue(lot.findSpot("F1-S1").orElseThrow().isOccupied());
    }

    @Test
    void parkIsEmptyWhenEverySpotIsTaken() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        Optional<Ticket> second = service.park(new Vehicle("KA-02-AA-9999", VehicleType.MEDIUM), ENTRY_GATE, RatePlan.HOURLY);

        assertTrue(second.isEmpty());
    }

    @Test
    void parkIsEmptyWhenNoFreeSpotFitsTheVehicle() {
        ParkingLotService service = serviceFor(lotOf(SpotType.SMALL, SpotType.LARGE));

        Optional<Ticket> ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY);

        assertTrue(ticket.isEmpty());
    }

    @Test
    void parkPicksTheSpotMatchingTheVehicleSize() {
        ParkingLot lot = lotOf(SpotType.SMALL, SpotType.LARGE, SpotType.MEDIUM);
        ParkingLotService service = serviceFor(lot);

        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        assertEquals("F1-S3", ticket.spotNumber());
        assertTrue(lot.findSpot("F1-S3").orElseThrow().isOccupied());
    }

    @Test
    void unparkFreesTheSpot() {
        ParkingLot lot = lotOf(SpotType.MEDIUM);
        ParkingLotService service = serviceFor(lot);
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        service.unpark(ticket.ticketId(), EXIT_GATE);

        assertEquals(1, lot.freeSpotCount(VehicleType.MEDIUM));
        assertFalse(lot.findSpot("F1-S1").orElseThrow().isOccupied());
    }

    @Test
    void receiptCarriesTheTicketAndTheClockExitTime() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();
        clock.advance(Duration.ofMinutes(45));

        Receipt receipt = service.unpark(ticket.ticketId(), EXIT_GATE).orElseThrow();

        assertSame(ticket, receipt.ticket());
        assertEquals(START.plus(Duration.ofMinutes(45)), receipt.exitTime());
        assertFalse(receipt.receiptId().isBlank());
    }

    @Test
    void anHourlyTicketIsPricedByTheHourlyCalculator() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();
        clock.advance(Duration.ofHours(25));

        BigDecimal price = service.unpark(ticket.ticketId(), EXIT_GATE).orElseThrow().price();

        assertEquals(0, price.compareTo(new BigDecimal("750")),
                () -> "25h hourly is 25 x 30 = 750, but was " + price);
    }

    @Test
    void aDailyTicketIsPricedByTheDailyCalculator() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.DAILY).orElseThrow();
        clock.advance(Duration.ofHours(25));

        BigDecimal price = service.unpark(ticket.ticketId(), EXIT_GATE).orElseThrow().price();

        assertEquals(0, price.compareTo(new BigDecimal("1000")),
                () -> "25h daily is 2 x 500 = 1000, but was " + price);
    }

    @Test
    void parkingThroughAnExitGateIsRejectedBeforeAnythingChanges() {
        ParkingLot lot = lotOf(SpotType.MEDIUM);
        ParkingLotService service = serviceFor(lot);

        assertThrows(IllegalArgumentException.class, () -> service.park(VEHICLE, EXIT_GATE, RatePlan.HOURLY));

        assertEquals(1, lot.freeSpotCount(VehicleType.MEDIUM));
        assertFalse(lot.findSpot("F1-S1").orElseThrow().isOccupied());
    }

    @Test
    void exitingThroughAnEntryGateIsRejectedAndTheCarStaysParked() {
        ParkingLot lot = lotOf(SpotType.MEDIUM);
        ParkingLotService service = serviceFor(lot);
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        assertThrows(IllegalArgumentException.class, () -> service.unpark(ticket.ticketId(), ENTRY_GATE));

        assertTrue(lot.findSpot("F1-S1").orElseThrow().isOccupied());
        assertTrue(ticketRepository.findById(ticket.ticketId()).isPresent());
    }

    @Test
    void theTicketIsStoredOnEntry() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));

        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        assertTrue(ticketRepository.findById(ticket.ticketId()).isPresent());
        assertSame(ticket, ticketRepository.findById(ticket.ticketId()).orElseThrow());
    }

    @Test
    void theTicketIsDiscardedOnExit() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        service.unpark(ticket.ticketId(), EXIT_GATE);

        assertTrue(ticketRepository.findById(ticket.ticketId()).isEmpty());
    }

    @Test
    void aDriverMayEnterAtOneGateAndLeaveAtAnother() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();

        Receipt receipt = service.unpark(ticket.ticketId(), EXIT_GATE).orElseThrow();

        assertSame(ticket, receipt.ticket());
        assertEquals("G1", receipt.ticket().gateNo());
        assertEquals("G2", receipt.gateNo());
    }

    @Test
    void unparkingAnUnknownTicketIdYieldsNoReceipt() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));

        assertTrue(service.unpark("not-a-ticket", EXIT_GATE).isEmpty());
    }

    @Test
    void unparkingTheSameTicketTwiceYieldsNoSecondReceipt() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        Ticket ticket = service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();
        service.unpark(ticket.ticketId(), EXIT_GATE);

        assertTrue(service.unpark(ticket.ticketId(), EXIT_GATE).isEmpty());
    }

    @Test
    void aStoredTicketPointingAtAnUnknownSpotIsRejected() {
        ParkingLotService service = serviceFor(lotOf(SpotType.MEDIUM));
        Ticket forged = new Ticket("T-1", "G1", "NOPE", "KA-01-HH-1234", START, RatePlan.HOURLY);
        ticketRepository.save(forged);

        assertThrows(IllegalStateException.class, () -> service.unpark("T-1", EXIT_GATE));
    }

    @Test
    void aFailedUnparkLeavesTheSpotOccupied() {
        ParkingLot lot = lotOf(SpotType.MEDIUM);
        ParkingLotService service = serviceFor(lot);
        service.park(VEHICLE, ENTRY_GATE, RatePlan.HOURLY).orElseThrow();
        Ticket forged = new Ticket("T-1", "G1", "NOPE", "KA-01-HH-1234", START, RatePlan.HOURLY);
        ticketRepository.save(forged);

        assertThrows(IllegalStateException.class, () -> service.unpark("T-1", EXIT_GATE));

        assertTrue(lot.findSpot("F1-S1").orElseThrow().isOccupied());
        assertEquals(0, lot.freeSpotCount(VehicleType.MEDIUM));
    }


    private ParkingLotService serviceFor(ParkingLot lot) {
        return new ParkingLotService(lot, clock, ticketRepository, CALCULATORS);
    }

    private ParkingLot lotOf(SpotType... spotTypes) {
        List<SpotSpec> specs = new ArrayList<>();
        for (int i = 0; i < spotTypes.length; i++) {
            specs.add(new SpotSpec(spotTypes[i], (i + 1) * 10));
        }
        return new ParkingLot("LOT-1",
                List.of(new ParkingFloor("F1", specs)),
                new FirstAvailableStrategy());
    }

    private static final class AdvanceableClock extends Clock {

        private Instant instant;

        private AdvanceableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration amount) {
            this.instant = this.instant.plus(amount);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("test clock is UTC only");
        }
    }
}
