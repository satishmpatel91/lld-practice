package com.parkinglot;

import com.parkinglot.constants.SpotType;
import com.parkinglot.constants.VehicleType;
import com.parkinglot.service.FirstAvailableStrategy;
import com.parkinglot.service.NearestToEntranceStrategy;
import com.parkinglot.service.SpotAllocationStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingLotTest {

    /** Distances ascend with position, so first-available and nearest agree. */
    private ParkingFloor floor(String floorNumber, SpotType... spotTypes) {
        List<SpotSpec> specs = new ArrayList<>();
        for (int i = 0; i < spotTypes.length; i++) {
            specs.add(new SpotSpec(spotTypes[i], (i + 1) * 10));
        }
        return new ParkingFloor(floorNumber, specs);
    }

    private ParkingFloor floor(String floorNumber, SpotSpec... specs) {
        return new ParkingFloor(floorNumber, List.of(specs));
    }

    private ParkingLot lotOf(ParkingFloor... floors) {
        return new ParkingLot("LOT-1", List.of(floors), new FirstAvailableStrategy());
    }

    private ParkingLot lotOf(SpotAllocationStrategy strategy, ParkingFloor... floors) {
        return new ParkingLot("LOT-1", List.of(floors), strategy);
    }

    @Test
    void claimFreeSpotReturnsTheFirstFittingSpotOnTheFirstFloorThatHasOne() {
        ParkingLot lot = lotOf(
                floor("F1", SpotType.MEDIUM, SpotType.MEDIUM),
                floor("F2", SpotType.MEDIUM));

        Optional<ParkingSpot> spot = lot.claimFreeSpot(VehicleType.MEDIUM);

        assertEquals("F1-S1", spot.orElseThrow().getSpotNumber());
    }

    @Test
    void claimFreeSpotMovesToTheNextFloorWhenTheFirstIsFull() {
        ParkingLot lot = lotOf(
                floor("F1", SpotType.MEDIUM),
                floor("F2", SpotType.MEDIUM));
        lot.findSpot("F1-S1").orElseThrow().tryOccupy();

        assertEquals("F2-S1", lot.claimFreeSpot(VehicleType.MEDIUM).orElseThrow().getSpotNumber());
    }

    @Test
    void claimFreeSpotMovesToTheNextFloorWhenTheFirstHasNoFittingSize() {
        ParkingLot lot = lotOf(
                floor("F1", SpotType.SMALL, SpotType.LARGE),
                floor("F2", SpotType.MEDIUM));

        assertEquals("F2-S1", lot.claimFreeSpot(VehicleType.MEDIUM).orElseThrow().getSpotNumber());
    }

    @Test
    void claimFreeSpotIsEmptyWhenNoFloorHasAFittingFreeSpot() {
        ParkingLot lot = lotOf(
                floor("F1", SpotType.SMALL),
                floor("F2", SpotType.SMALL));

        assertTrue(lot.claimFreeSpot(VehicleType.LARGE).isEmpty());
        assertEquals(0, lot.freeSpotCount(VehicleType.LARGE));
        assertEquals(2, lot.freeSpotCount(VehicleType.SMALL));
    }

    @Test
    void claimingTwiceNeverHandsOutTheSameSpot() {
        ParkingLot lot = lotOf(floor("F1", SpotType.MEDIUM));

        ParkingSpot first = lot.claimFreeSpot(VehicleType.MEDIUM).orElseThrow();

        assertTrue(first.isOccupied());
        assertTrue(lot.claimFreeSpot(VehicleType.MEDIUM).isEmpty());
    }

    @Test
    void aClaimedSpotIsAlreadyOccupiedSoTheCallerNeedNotOccupyIt() {
        ParkingLot lot = lotOf(floor("F1", SpotType.MEDIUM, SpotType.MEDIUM));

        ParkingSpot spot = lot.claimFreeSpot(VehicleType.MEDIUM).orElseThrow();

        assertTrue(spot.isOccupied());
        assertEquals(1, lot.freeSpotCount(VehicleType.MEDIUM));
    }

    @Test
    void claimFreeSpotIsEmptyWhenTheLotHasNoFloors() {
        ParkingLot lot = lotOf();

        assertTrue(lot.claimFreeSpot(VehicleType.MEDIUM).isEmpty());
        assertEquals(0, lot.freeSpotCount(VehicleType.MEDIUM));
    }

    @Test
    void theAllocationPolicyDecidesWhichSpotTheSameLotHandsOut() {
        ParkingFloor ground = floor("F1",
                new SpotSpec(SpotType.MEDIUM, 90),
                new SpotSpec(SpotType.MEDIUM, 70));
        ParkingFloor upper = floor("F2",
                new SpotSpec(SpotType.MEDIUM, 5));

        ParkingLot firstAvailable = new ParkingLot("LOT-1",
                List.of(ground, upper), new FirstAvailableStrategy());
        ParkingSpot byOrder = firstAvailable.claimFreeSpot(VehicleType.MEDIUM).orElseThrow();

        ParkingFloor ground2 = floor("F1",
                new SpotSpec(SpotType.MEDIUM, 90),
                new SpotSpec(SpotType.MEDIUM, 70));
        ParkingFloor upper2 = floor("F2",
                new SpotSpec(SpotType.MEDIUM, 5));
        ParkingLot nearest = new ParkingLot("LOT-1",
                List.of(ground2, upper2), new NearestToEntranceStrategy());
        ParkingSpot byDistance = nearest.claimFreeSpot(VehicleType.MEDIUM).orElseThrow();

        assertEquals("F1-S1", byOrder.getSpotNumber());
        assertEquals("F2-S1", byDistance.getSpotNumber());
        assertNotEquals(byOrder.getSpotNumber(), byDistance.getSpotNumber(),
                "if both policies agree, the layout cannot tell them apart");
    }

    @Test
    void findSpotLocatesASpotOnAnyFloor() {
        ParkingLot lot = lotOf(
                floor("F1", SpotType.MEDIUM),
                floor("F2", SpotType.MEDIUM, SpotType.LARGE));

        assertEquals("F2-S2", lot.findSpot("F2-S2").orElseThrow().getSpotNumber());
        assertEquals("F2", lot.findSpot("F2-S2").orElseThrow().getFloorNumber());
    }

    @Test
    void findSpotIsEmptyForAnUnknownSpotNumber() {
        ParkingLot lot = lotOf(floor("F1", SpotType.MEDIUM));

        assertTrue(lot.findSpot("F9-S9").isEmpty());
    }

    @Test
    void freeSpotCountSumsEveryFloor() {
        ParkingLot lot = lotOf(
                floor("F1", SpotType.MEDIUM, SpotType.LARGE),
                floor("F2", SpotType.MEDIUM, SpotType.MEDIUM));

        assertEquals(3, lot.freeSpotCount(VehicleType.MEDIUM));
        assertEquals(1, lot.freeSpotCount(VehicleType.LARGE));
    }

    @Test
    void freeSpotCountTracksOccupancyAcrossFloors() {
        ParkingLot lot = lotOf(
                floor("F1", SpotType.MEDIUM),
                floor("F2", SpotType.MEDIUM));
        assertEquals(2, lot.freeSpotCount(VehicleType.MEDIUM));

        ParkingSpot spot = lot.findSpot("F2-S1").orElseThrow();
        spot.tryOccupy();
        assertEquals(1, lot.freeSpotCount(VehicleType.MEDIUM));

        spot.free();
        assertEquals(2, lot.freeSpotCount(VehicleType.MEDIUM));
    }

    @Test
    void lotIsUnaffectedByLaterChangesToTheFloorListItWasBuiltFrom() {
        List<ParkingFloor> floors = new ArrayList<>(List.of(floor("F1", SpotType.MEDIUM)));
        ParkingLot lot = new ParkingLot("LOT-1", floors, new FirstAvailableStrategy());

        floors.add(floor("F2", SpotType.MEDIUM));

        assertEquals(1, lot.freeSpotCount(VehicleType.MEDIUM));
        assertTrue(lot.findSpot("F2-S1").isEmpty());
    }
}
