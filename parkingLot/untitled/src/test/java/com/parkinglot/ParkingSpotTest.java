package com.parkinglot;

import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingSpotTest {

    @Test
    void newSpotIsFree() {
        ParkingSpot spot = new ParkingSpot("F1-S1", "F1", SpotType.MEDIUM, 10);

        assertFalse(spot.isOccupied());
        assertEquals("F1-S1", spot.getSpotNumber());
        assertEquals("F1", spot.getFloorNumber());
        assertEquals(SpotType.MEDIUM, spot.getSpotType());
        assertEquals(10, spot.getDistanceFromEntrance());
    }

    @Test
    void occupyMarksSpotOccupied() {
        ParkingSpot spot = new ParkingSpot("F1-S1", "F1", SpotType.MEDIUM, 10);

        spot.tryOccupy();

        assertTrue(spot.isOccupied());
    }

    @Test
    void freeMakesSpotAvailableAgain() {
        ParkingSpot spot = new ParkingSpot("F1-S1", "F1", SpotType.MEDIUM, 10);
        spot.tryOccupy();

        spot.free();

        assertFalse(spot.isOccupied());
    }

    @Test
    void claimingAnOccupiedSpotLosesInsteadOfThrowing() {
        ParkingSpot spot = new ParkingSpot("F1-S1", "F1", SpotType.MEDIUM, 10);

        assertTrue(spot.tryOccupy());
        assertFalse(spot.tryOccupy());
        assertTrue(spot.isOccupied());
    }

    @Test
    void freeingAFreeSpotIsRejected() {
        ParkingSpot spot = new ParkingSpot("F1-S1", "F1", SpotType.MEDIUM, 10);

        assertThrows(IllegalStateException.class, spot::free);
        assertFalse(spot.isOccupied());
    }

    @Test
    void aFreeSpotFitsAVehicleOfItsOwnSize() {
        assertTrue(new ParkingSpot("F1-S1", "F1", SpotType.SMALL, 10).canFit(VehicleType.SMALL));
        assertTrue(new ParkingSpot("F1-S2", "F1", SpotType.MEDIUM, 10).canFit(VehicleType.MEDIUM));
        assertTrue(new ParkingSpot("F1-S3", "F1", SpotType.LARGE, 10).canFit(VehicleType.LARGE));
    }

    @Test
    void aSpotDoesNotFitALargerVehicle() {
        assertFalse(new ParkingSpot("F1-S1", "F1", SpotType.SMALL, 10).canFit(VehicleType.MEDIUM));
        assertFalse(new ParkingSpot("F1-S2", "F1", SpotType.MEDIUM, 10).canFit(VehicleType.LARGE));
    }

    @Test
    void aSpotDoesNotFitASmallerVehicle() {
        assertFalse(new ParkingSpot("F1-S1", "F1", SpotType.LARGE, 10).canFit(VehicleType.MEDIUM));
        assertFalse(new ParkingSpot("F1-S2", "F1", SpotType.MEDIUM, 10).canFit(VehicleType.SMALL));
    }
}
