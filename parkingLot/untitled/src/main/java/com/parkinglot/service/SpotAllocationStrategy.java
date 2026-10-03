package com.parkinglot.service;

import com.parkinglot.ParkingSpot;

import java.util.List;
import java.util.Optional;

// com.parkinglot.service.SpotAllocationStrategy
public interface SpotAllocationStrategy {

    Optional<ParkingSpot> claimFrom(List<ParkingSpot> candidates);

    default Optional<ParkingSpot> claimInOrder(List<ParkingSpot> ordered) {
        for (ParkingSpot spot : ordered) {
            if (spot.tryOccupy()) {
                return Optional.of(spot);
            }
        }
        return Optional.empty();
    }
}