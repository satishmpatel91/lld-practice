package com.parkinglot;

import com.parkinglot.constants.VehicleType;
import com.parkinglot.service.SpotAllocationStrategy;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ParkingLot {

    @Getter
    private final String lotNumber;
    private final List<ParkingFloor> floors;
    private final SpotAllocationStrategy allocationStrategy;

    public ParkingLot(String lotNumber, List<ParkingFloor> floors, SpotAllocationStrategy allocationStrategy) {
        this.lotNumber = lotNumber;
        this.floors = List.copyOf(floors);
        this.allocationStrategy = Objects.requireNonNull(allocationStrategy, "allocationStrategy");
    }

    public Optional<ParkingSpot> claimFreeSpot(VehicleType vehicleType) {
        List<ParkingSpot> candidates = floors.stream()
                .flatMap(floor -> floor.freeSpotsFor(vehicleType).stream())
                .toList();

        return allocationStrategy.claimFrom(candidates);
    }

    public Optional<ParkingSpot> findSpot(String spotNumber) {
        return floors.stream()
                .map(floor -> floor.findSpot(spotNumber))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public int freeSpotCount(VehicleType vehicleType) {
        return floors.stream()
                .mapToInt(floor -> floor.freeSpotCount(vehicleType))
                .sum();
    }
}
