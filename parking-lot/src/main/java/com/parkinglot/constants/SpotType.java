package com.parkinglot.constants;

import java.util.Set;

public enum SpotType {
    SMALL(Set.of(VehicleType.SMALL)),
    MEDIUM(Set.of(VehicleType.MEDIUM)),
    LARGE(Set.of(VehicleType.LARGE));

    private final Set<VehicleType> acceptedVehicleTypes;

    SpotType(Set<VehicleType> acceptedVehicleTypes) {
        this.acceptedVehicleTypes = acceptedVehicleTypes;
    }

    public boolean accepts(VehicleType vehicleType) {
        return acceptedVehicleTypes.contains(vehicleType);
    }

}
