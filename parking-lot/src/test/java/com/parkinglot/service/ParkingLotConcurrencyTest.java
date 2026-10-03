package com.parkinglot.service;

import com.parkinglot.Gate;
import com.parkinglot.ParkingFloor;
import com.parkinglot.ParkingLot;
import com.parkinglot.Ticket;
import com.parkinglot.SpotSpec;
import com.parkinglot.Vehicle;
import com.parkinglot.constants.GateType;
import com.parkinglot.constants.RatePlan;
import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;
import com.parkinglot.repo.TicketRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingLotConcurrencyTest {

    private static final int SPOTS_PER_FLOOR = 25;
    private static final int FLOORS = 2;
    private static final int TOTAL_SPOTS = SPOTS_PER_FLOOR * FLOORS;
    private static final int DRIVERS = 200;

    private static final Gate ENTRY_GATE = new Gate("G1", GateType.ENTRY);
    private static final BigDecimal RATE_PER_HOUR = new BigDecimal("30");
    private static final Map<RatePlan, FeeCalculator> CALCULATORS = Map.of(
            RatePlan.HOURLY, new HourlyFeeCalculator(RATE_PER_HOUR),
            RatePlan.DAILY, new DailyFeeCalculator(new BigDecimal("500")));

    private static List<SpotSpec> mediumSpots(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new SpotSpec(SpotType.MEDIUM, i * 10))
                .collect(Collectors.toList());
    }

    @Test
    void concurrentDriversNeverShareASpot() throws Exception {
        ParkingLot lot = new ParkingLot("LOT-1", IntStream.rangeClosed(1, FLOORS)
                .mapToObj(f -> new ParkingFloor("F" + f, mediumSpots(SPOTS_PER_FLOOR)))
                .collect(Collectors.toList()),
                new FirstAvailableStrategy());

        TicketRepository ticketRepository = new TicketRepository();
        ParkingLotService service =
                new ParkingLotService(lot, Clock.systemUTC(), ticketRepository, CALCULATORS);

        Set<String> claimedSpots = ConcurrentHashMap.newKeySet();
        AtomicInteger admitted = new AtomicInteger();
        AtomicInteger turnedAway = new AtomicInteger();
        AtomicInteger doubleClaims = new AtomicInteger();

        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(DRIVERS);
        ExecutorService pool = Executors.newFixedThreadPool(32);

        for (int i = 0; i < DRIVERS; i++) {
            String plate = "KA-01-" + i;
            pool.execute(() -> {
                try {
                    startGun.await();
                    Optional<Ticket> ticket =
                            service.park(new Vehicle(plate, VehicleType.MEDIUM), ENTRY_GATE, RatePlan.HOURLY);
                    if (ticket.isPresent()) {
                        admitted.incrementAndGet();
                        if (!claimedSpots.add(ticket.get().spotNumber())) {
                            doubleClaims.incrementAndGet();
                        }
                    } else {
                        turnedAway.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGun.countDown();
        assertTrue(finished.await(30, TimeUnit.SECONDS), "drivers did not finish in time");
        pool.shutdownNow();

        assertEquals(0, doubleClaims.get(), "two drivers were issued the same spot");
        assertEquals(TOTAL_SPOTS, admitted.get(), "every spot should be filled exactly once");
        assertEquals(DRIVERS - TOTAL_SPOTS, turnedAway.get());
        assertEquals(TOTAL_SPOTS, claimedSpots.size());
        assertEquals(0, lot.freeSpotCount(VehicleType.MEDIUM));
    }

    @Test
    void everyIssuedTicketSurvivesConcurrentEntry() throws Exception {
        ParkingLot lot = new ParkingLot("LOT-1",
                List.of(new ParkingFloor("F1", mediumSpots(TOTAL_SPOTS))),
                new FirstAvailableStrategy());

        TicketRepository ticketRepository = new TicketRepository();
        ParkingLotService service =
                new ParkingLotService(lot, Clock.systemUTC(), ticketRepository, CALCULATORS);

        Set<String> issuedTicketIds = ConcurrentHashMap.newKeySet();
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(DRIVERS);
        ExecutorService pool = Executors.newFixedThreadPool(32);

        for (int i = 0; i < DRIVERS; i++) {
            String plate = "KA-02-" + i;
            pool.execute(() -> {
                try {
                    startGun.await();
                    service.park(new Vehicle(plate, VehicleType.MEDIUM), ENTRY_GATE, RatePlan.HOURLY)
                            .ifPresent(ticket -> issuedTicketIds.add(ticket.ticketId()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGun.countDown();
        assertTrue(finished.await(30, TimeUnit.SECONDS), "drivers did not finish in time");
        pool.shutdownNow();

        assertEquals(TOTAL_SPOTS, issuedTicketIds.size());
        for (String ticketId : issuedTicketIds) {
            assertTrue(ticketRepository.findById(ticketId).isPresent(),
                    "ticket " + ticketId + " was issued but is not in the repository");
        }
    }
}
