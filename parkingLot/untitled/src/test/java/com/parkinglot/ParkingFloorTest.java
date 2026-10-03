package com.parkinglot;

import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingFloorTest {

    private ParkingFloor floor(String floorNumber, SpotType... spotTypes) {
        List<SpotSpec> specs = new ArrayList<>();
        for (int i = 0; i < spotTypes.length; i++) {
            specs.add(new SpotSpec(spotTypes[i], (i + 1) * 10));
        }
        return new ParkingFloor(floorNumber, specs);
    }

    @Test
    void freeSpotsForReturnsOnlyTheFittingSpots() {
        ParkingFloor floor = floor("F1", SpotType.LARGE, SpotType.MEDIUM, SpotType.MEDIUM);

        List<ParkingSpot> candidates = floor.freeSpotsFor(VehicleType.MEDIUM);

        assertEquals(List.of("F1-S2", "F1-S3"),
                candidates.stream().map(ParkingSpot::getSpotNumber).toList());
    }

    @Test
    void freeSpotsForExcludesOccupiedSpots() {
        ParkingFloor floor = floor("F1", SpotType.MEDIUM, SpotType.MEDIUM);
        floor.findSpot("F1-S1").orElseThrow().tryOccupy();

        List<ParkingSpot> candidates = floor.freeSpotsFor(VehicleType.MEDIUM);

        assertEquals(List.of("F1-S2"),
                candidates.stream().map(ParkingSpot::getSpotNumber).toList());
    }

    @Test
    void freeSpotsForIsEmptyWhenNothingOnThisFloorFits() {
        ParkingFloor floor = floor("F1", SpotType.SMALL, SpotType.SMALL);

        assertTrue(floor.freeSpotsFor(VehicleType.LARGE).isEmpty());
    }

    @Test
    void freeSpotsForHandsOutADerivedListNotTheFloorsOwnCollection() {
        ParkingFloor floor = floor("F1", SpotType.MEDIUM);

        List<ParkingSpot> candidates = floor.freeSpotsFor(VehicleType.MEDIUM);

        assertThrows(UnsupportedOperationException.class,
                () -> candidates.add(new ParkingSpot("X", "F1", SpotType.MEDIUM, 1)));
        assertEquals(1, floor.freeSpotCount(VehicleType.MEDIUM));
    }

    @Test
    void findSpotResolvesASpotNumberOnThisFloor() {
        ParkingFloor floor = floor("F1", SpotType.MEDIUM, SpotType.LARGE);

        assertEquals("F1-S2", floor.findSpot("F1-S2").orElseThrow().getSpotNumber());
    }

    @Test
    void findSpotIsEmptyForASpotOnAnotherFloor() {
        ParkingFloor floor = floor("F1", SpotType.MEDIUM);

        assertTrue(floor.findSpot("F2-S1").isEmpty());
    }

    @Test
    void freeSpotCountCountsOnlyFittingFreeSpotsOnThisFloor() {
        ParkingFloor floor = floor("F1", SpotType.MEDIUM, SpotType.MEDIUM, SpotType.LARGE);
        assertEquals(2, floor.freeSpotCount(VehicleType.MEDIUM));

        floor.findSpot("F1-S1").orElseThrow().tryOccupy();

        assertEquals(1, floor.freeSpotCount(VehicleType.MEDIUM));
        assertEquals(1, floor.freeSpotCount(VehicleType.LARGE));
    }

    @Test
    void floorIsUnaffectedByLaterChangesToTheListItWasBuiltFrom() {
        List<SpotSpec> specs = new ArrayList<>(List.of(new SpotSpec(SpotType.MEDIUM, 10)));
        ParkingFloor floor = new ParkingFloor("F1", specs);

        specs.add(new SpotSpec(SpotType.MEDIUM, 20));

        assertEquals(1, floor.freeSpotCount(VehicleType.MEDIUM));
        assertTrue(floor.findSpot("F1-S2").isEmpty());
    }

    @Test
    void theFloorNamesItsOwnSpotsSoTheIdAlwaysMatchesTheFloor() {
        ParkingFloor floor = floor("F7", SpotType.MEDIUM, SpotType.LARGE);

        assertEquals("F7-S1", floor.findSpot("F7-S1").orElseThrow().getSpotNumber());
        assertEquals("F7", floor.findSpot("F7-S2").orElseThrow().getFloorNumber());
    }

    @Test
    void theFloorAppliesTheDistanceGivenForEachSpot() {
        ParkingFloor floor = new ParkingFloor("F1", List.of(
                new SpotSpec(SpotType.MEDIUM, 90),
                new SpotSpec(SpotType.MEDIUM, 4)));

        assertEquals(90, floor.findSpot("F1-S1").orElseThrow().getDistanceFromEntrance());
        assertEquals(4, floor.findSpot("F1-S2").orElseThrow().getDistanceFromEntrance());
    }

    @Test
    void aNullSpotSpecIsRejected() {
        List<SpotSpec> withNull = Arrays.asList(new SpotSpec(SpotType.MEDIUM, 10), null);

        assertThrows(IllegalArgumentException.class, () -> new ParkingFloor("F1", withNull));
    }
}
