package com.parkinglot.service;

import com.parkinglot.ParkingSpot;
import com.parkinglot.constants.SpotType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearestToEntranceStrategyTest {

    private final SpotAllocationStrategy strategy = new NearestToEntranceStrategy();

    @Test
    void claimsTheNearestCandidateNotTheFirstOne() {
        ParkingSpot far = spot("F1-S1", 100);
        ParkingSpot nearest = spot("F2-S7", 1);
        ParkingSpot middle = spot("F1-S2", 50);

        ParkingSpot claimed = strategy.claimFrom(List.of(far, nearest, middle)).orElseThrow();

        assertEquals("F2-S7", claimed.getSpotNumber());
        assertTrue(nearest.isOccupied());
        assertFalse(far.isOccupied());
        assertFalse(middle.isOccupied());
    }

    @Test
    void distanceWinsOverFloorOrder() {
        ParkingSpot groundFloorFar = spot("F1-S1", 90);
        ParkingSpot topFloorClose = spot("F9-S1", 2);

        ParkingSpot claimed = strategy.claimFrom(List.of(groundFloorFar, topFloorClose)).orElseThrow();

        assertEquals("F9-S1", claimed.getSpotNumber());
    }

    @Test
    void fallsBackToTheNextNearestWhenTheNearestIsTaken() {
        ParkingSpot nearest = spot("F1-S1", 5);
        ParkingSpot secondNearest = spot("F1-S2", 9);
        ParkingSpot far = spot("F1-S3", 90);
        nearest.tryOccupy();

        ParkingSpot claimed = strategy.claimFrom(List.of(far, nearest, secondNearest)).orElseThrow();

        assertEquals("F1-S2", claimed.getSpotNumber());
    }

    @Test
    void anImmutableCandidateListIsAccepted() {
        List<ParkingSpot> immutable = List.of(spot("F1-S1", 40), spot("F1-S2", 10));

        ParkingSpot claimed = strategy.claimFrom(immutable).orElseThrow();

        assertEquals("F1-S2", claimed.getSpotNumber());
        assertEquals("F1-S1", immutable.get(0).getSpotNumber(), "the caller's list must not be reordered");
    }

    @Test
    void isEmptyWhenEveryCandidateIsAlreadyTaken() {
        ParkingSpot taken = spot("F1-S1", 10);
        taken.tryOccupy();

        assertTrue(strategy.claimFrom(List.of(taken)).isEmpty());
    }

    @Test
    void isEmptyWhenThereAreNoCandidates() {
        assertTrue(strategy.claimFrom(List.of()).isEmpty());
    }

    private ParkingSpot spot(String spotNumber, int distance) {
        return new ParkingSpot(spotNumber, spotNumber.substring(0, 2), SpotType.MEDIUM, distance);
    }
}
