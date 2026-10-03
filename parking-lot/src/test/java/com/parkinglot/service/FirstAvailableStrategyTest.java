package com.parkinglot.service;

import com.parkinglot.ParkingSpot;
import com.parkinglot.constants.SpotType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstAvailableStrategyTest {

    private final SpotAllocationStrategy strategy = new FirstAvailableStrategy();

    @Test
    void claimsTheFirstCandidateInTheOrderGiven() {
        ParkingSpot near = spot("F1-S1", 100);
        ParkingSpot far = spot("F1-S2", 1);

        ParkingSpot claimed = strategy.claimFrom(List.of(near, far)).orElseThrow();

        assertEquals("F1-S1", claimed.getSpotNumber());
        assertTrue(near.isOccupied());
        assertTrue(!far.isOccupied());
    }

    @Test
    void theClaimedSpotIsAlreadyOccupiedOnReturn() {
        ParkingSpot only = spot("F1-S1", 10);

        ParkingSpot claimed = strategy.claimFrom(List.of(only)).orElseThrow();

        assertTrue(claimed.isOccupied());
    }

    @Test
    void skipsACandidateAnotherThreadAlreadyClaimed() {
        ParkingSpot taken = spot("F1-S1", 10);
        ParkingSpot free = spot("F1-S2", 20);
        taken.tryOccupy();

        ParkingSpot claimed = strategy.claimFrom(List.of(taken, free)).orElseThrow();

        assertEquals("F1-S2", claimed.getSpotNumber());
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
