package com.parkinglot.service;

import com.parkinglot.ParkingSpot;

import java.util.List;
import java.util.Optional;

public class FirstAvailableStrategy implements SpotAllocationStrategy {

    @Override
    public Optional<ParkingSpot> claimFrom(List<ParkingSpot> candidates) {
        return claimInOrder(candidates);
    }
}