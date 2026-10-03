package com.parkinglot;

import com.parkinglot.constants.VehicleType;

public record Vehicle(
        String vehicleNumber,
        VehicleType vehicleType
) {
}
