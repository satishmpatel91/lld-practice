package com.parkinglot;

import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ParkingFloor {

    private final List<ParkingSpot> spots;

    public ParkingFloor(String floorNumber, List<SpotSpec> spotSpecs) {

        List<ParkingSpot> built = new ArrayList<>();
        for (int position = 1; position <= spotSpecs.size(); position++) {
            SpotSpec spotSpec = spotSpecs.get(position - 1);
            if (spotSpec == null) {
                throw new IllegalArgumentException("Spot spec cannot be null");
            }
            built.add(new ParkingSpot(
                    floorNumber + "-S" + position,
                    floorNumber,
                    spotSpec.type(),
                    spotSpec.distanceFromEntrance()));
        }
        this.spots = List.copyOf(built);
    }

    public List<ParkingSpot> freeSpotsFor(VehicleType vehicleType) {
        return spots.stream()
                .filter(spot -> spot.canFit(vehicleType) && !spot.isOccupied())
                .toList();
    }

    public Optional<ParkingSpot> findSpot(String spotNumber) {
        return spots.stream()
                .filter(spot -> spot.getSpotNumber().equals(spotNumber))
                .findFirst();
    }

    public int freeSpotCount(VehicleType vehicleType) {
        return (int) spots.stream()
                .filter(spot -> spot.canFit(vehicleType) && !spot.isOccupied())
                .count();
    }

    public Optional<ParkingSpot> claimFreeSpot(VehicleType vehicleType) {
        for (ParkingSpot spot : spots) {
            if (spot.canFit(vehicleType) && spot.tryOccupy()) {
                return Optional.of(spot);
            }
        }
        return Optional.empty();
    }
}
