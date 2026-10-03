package com.parkinglot;

import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParkingSpot {

    @Getter
    private final String spotNumber;
    @Getter
    private final String floorNumber;
    @Getter
    private final SpotType spotType;

    @Getter
    private final int distanceFromEntrance;

    private final AtomicBoolean occupied = new AtomicBoolean(false);

    public ParkingSpot(String spotNumber, String floorNumber, SpotType spotType, int distanceFromEntrance) {
        this.distanceFromEntrance = distanceFromEntrance;
        this.spotNumber = spotNumber;
        this.floorNumber = floorNumber;
        this.spotType = spotType;
    }

    public boolean canFit(VehicleType vehicleType) {
        return spotType.accepts(vehicleType);
    }

    public boolean tryOccupy() {
        return occupied.compareAndSet(false, true);
    }

    public boolean isOccupied() {
        return occupied.get();
    }

    public void free() {
        if (!occupied.compareAndSet(true, false)) {
            throw new IllegalStateException("Parking spot is already free");
        }
    }
}
