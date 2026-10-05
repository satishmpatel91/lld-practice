# Parking Lot, Eight Steps — interactive walkthrough

`index.html` is a single self-contained page covering the same build order as
[`../ParkingLot.md`](../ParkingLot.md), but by letting you work the gates
instead of reading about them.

Open it straight from disk — no build step, no dependencies, no server:

```
xdg-open animation/index.html
```

Also published as a private Artifact:
<https://claude.ai/artifact/8KC5MjGfAy1AS9swSAMexX>

## What it shows

A garage seen from above — levels, bays with their size and distance from the
entrance, and entry/exit barriers that lift as vehicles pass. Pick a vehicle,
park it, advance the clock, drive it out. Then change step and try the same
thing again.

**Several vehicles can be in the lot at once.** Each one gets its own ticket in
the controls panel, carrying its own entry time, so one lot clock serves them
all and each receipt prices only that vehicle's stay — the same shape as
`TicketRepository` holding many tickets. **Fill the lot** parks vehicles until
`claimFreeSpot` refuses, which is how a full lot reports itself: an empty
`Optional`, not an exception.

| Try this | What it demonstrates |
|---|---|
| Step 1, park a truck | every bay accepts every vehicle, because there is only one kind of bay |
| Step 2, park a truck | `SpotType.accepts(vehicleType)` turns it away unless a large bay is free |
| Step 3, park using gate G2 | `requireGateType` throws: a miswired gate is a programming mistake, while a full lot is an empty `Optional` |
| Step 6, switch strategy | the candidate bays get numbered in the order the strategy saw them, so first-available and nearest-to-entrance pick differently |
| Step 5, park then +26h | the receipt shows chargeable units, and that part periods round up |

## The two benches further down the page

**Ceiling a division.** Drag the stay and compare the correct formula against
the two that shipped first. At 47h30 all three disagree: `ceil((double) m /
1440)` gives 600, `toDays()` gives 300, and `ceil(m / 1440)` with long division
also gives 300 — in a line that visibly contains `Math.ceil` and therefore looks
right. The numbers on the page are computed with the same formulas as
`HourlyFeeCalculator` and `DailyFeeCalculator`.

**Find, then claim.** Toggle between `findFreeSpot` followed by `occupy` (two
calls with a gap, which prints two tickets for one bay) and `claimFreeSpot`
(one call, compare-and-set, so the loser simply gets `false` back and tries the
next candidate). Below it, the `compareAndSet` return-value bug and why it did
more damage than a crash.

## Notes

- One file, no libraries. Fonts come from Google Fonts; everything else is inline.
- Light and dark themes, works at phone width, honours `prefers-reduced-motion`
  by running the animations instantly.
- The clock and the rates are illustrative. The real service takes an injected
  `Clock`, which is why the fee tests run instantly instead of sleeping.
