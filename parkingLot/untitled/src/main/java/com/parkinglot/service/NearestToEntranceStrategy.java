package com.parkinglot.service;

import com.parkinglot.ParkingSpot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NearestToEntranceStrategy implements SpotAllocationStrategy {

    @Override
    public Optional<ParkingSpot> claimFrom(List<ParkingSpot> candidates) {
        List<ParkingSpot> byDistance = new ArrayList<>(candidates);
        byDistance.sort(Comparator.comparingInt(ParkingSpot::getDistanceFromEntrance));
        return claimInOrder(byDistance);
    }
}