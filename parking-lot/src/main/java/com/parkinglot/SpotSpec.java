package com.parkinglot;

import com.parkinglot.constants.SpotType;

// com.parkinglot.SpotSpec
public record SpotSpec(SpotType type, int distanceFromEntrance) {
}