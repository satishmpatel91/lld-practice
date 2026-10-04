# Vending Machine — Low Level Design

Built evolutionarily: V1 → V5. Each version solves a real problem created by the
previous one. No pattern is introduced before the pain that justifies it.

---

## Behaviour space (full menu, before scoping)

**User:** view items and prices · select by code · insert money (coins / notes /
card) · receive item · receive change · cancel and get refund

**Machine:** track inventory per slot · validate selection and stock · validate
amount · compute change · verify change is physically possible · enforce state
transitions · reject invalid transitions

**Operator:** restock · reprice · collect cash · refill change float · sales reports

**Production:** concurrent users · payment failure after money taken · refunds ·
idempotency · dispense jams · logging and metrics

The two genuinely hard parts of this problem are the **state machine** and
**change-making** (which can legitimately fail even when the user paid enough).

---

## V1 — Basic Design

### Scope

| Behaviour | In V1 |
|---|---|
| Show items | yes — code, name, price, quantity |
| Select item | yes — must exist, must be in stock |
| Insert money | yes — as a plain amount, not denominations |
| Dispense | yes — item out, plus change as an amount |
| Change can fail | **no** |

**Deliberately excluded:** denomination handling, coin float, cancel/refund,
restock, card payment, reports, concurrency.

**Stated assumption:** the coin float is infinite, so change is always possible.
V1 returns change as a number, not as physical coins. Say this out loud in an
interview — it converts a hole into a declared boundary.

**Deleted on purpose:** a `Payment` class. With money as a single integer, it
would hold nothing an `int amountInserted` field does not already hold. It comes
back in V3, when card payments make "how you paid" a real variation.

### Classes

| Class | Responsibility |
|---|---|
| `VendingMachine` | Entry point; holds the in-progress transaction (`selectedSlot`, `amountInserted`) and runs the four operations |
| `Inventory` | Owns all slots; looks up a slot by code |
| `Slot` | A physical position (`A1`) holding one item type and a quantity |
| `Item` | A product definition: name and price |
| `DispenseResult` | The outcome of a purchase: which item, how much change |
| `ItemView` | Read-only display row: slot code, name, price, availability |

`Item` and `Slot` are deliberately separate. `Coke` is a product with a name and
a price; `A1` is a location holding *some number of* Cokes. Collapsing them
breaks as soon as two slots stock the same product.

### Class diagram

```mermaid
classDiagram
    class VendingMachine {
        -Inventory inventory
        -Slot selectedSlot
        -int amountInserted
        +showItems() List~ItemView~
        +selectItem(String code) void
        +insertMoney(int amount) void
        +dispense() DispenseResult
        +cancel() int
    }

    class Inventory {
        -Map~String, Slot~ slots
        +getSlot(String code) Slot
        +allSlots() Collection~Slot~
    }

    class Slot {
        -String code
        -Item item
        -int quantity
        +reduceQuantity() void
        +isEmpty() boolean
    }

    class Item {
        -String name
        -int price
    }

    class DispenseResult {
        <<record>>
        +Item item
        +int change
    }

    class ItemView {
        <<record>>
        +String slotCode
        +String name
        +int price
        +boolean available
        +fromSlot(Slot slot) ItemView
    }

    VendingMachine *-- Inventory : composition
    Inventory *-- "1..*" Slot : composition
    Slot o-- Item : aggregation
    VendingMachine --> Slot : selectedSlot
    VendingMachine ..> DispenseResult : dependency
    VendingMachine ..> ItemView : dependency
    ItemView ..> Slot : maps from
```

Relationship choices worth defending: `Inventory` **composes** its slots (they
do not outlive the machine), while a `Slot` only **aggregates** its `Item` (the
product definition exists independently of any slot).

### Flow

```
showItems()        -> A1 Coke  25  (qty 3)
                      A2 Chips 20  (qty 0)
selectItem("A1")   -> selectedSlot = A1
insertMoney(50)    -> amountInserted = 50
dispense()         -> 50 >= 25 ok -> qty 3 -> 2, change = 25
                   -> selectedSlot = null, amountInserted = 0
```

### Problems with V1

**The disease: no state validation.** V1 accepts illegal call sequences and
produces nonsense:

| Sequence | What V1 does | What it should do |
|---|---|---|
| `insertMoney(50)` then `dispense()` | dispenses nothing meaningful — nothing was selected | reject: no selection |
| `selectItem("A1")` then `dispense()` | dispenses an unpaid item | reject: no payment |
| `selectItem("A1")`, `insertMoney(10)`, `dispense()` | takes 10 for a 25 item | reject: insufficient amount |

**Where the fix is forced to live:** `VendingMachine`, because nothing else
knows the transaction. Every operation grows a guard clause at the top that
infers the current state from two fields — `selectedSlot == null` and
`amountInserted >= price`.

**Tight coupling.** The transition rules are welded into the bodies of the four
operations. There is no object you can point at and say "this is the rule".

**Hard to extend.** Adding `cancel()` is not just a new method — it forces a
re-reading of the guards inside every existing method ("can I cancel from idle?
can I insert money after cancelling?"). Each answer is another `if` in another
place. Guard logic grows with states x operations.

**SOLID violations.**
- **OCP** — every new state or operation requires editing `VendingMachine` and
  re-reasoning guards that already worked.
- **SRP** — `VendingMachine` both orchestrates a purchase *and* is the sole
  authority on legal transitions. Two reasons to change in one class.

**Implicit state is the root cause.** The state is not represented anywhere; it
is *inferred* from field values. Three states can be faked this way. More cannot
— `DISPENSING` and `OUT_OF_SERVICE` cannot be expressed by those two fields at
all, and field combinations start producing states that should be impossible.

**Design note surfaced here:** decrement quantity at **dispense**, not at
selection. If selection decremented, `cancel()` would have to restore stock —
extra state to unwind for no gain.

---

## V2a — Explicit state, derived (enum)

### Problem

V1 is correct but its state is **implicit**: no object or field says where the
machine is. Every operation re-derives the answer inline from
`selectedSlot == null` and `amountInserted`, so seven state checks sit scattered
across four method bodies, each coupled to the field layout.

### First, a modelling decision

The states are `IDLE` and `ITEM_SELECTED`. **Not** `PAID` — "the user has paid
enough" is a *condition*, not a state:

> A state you have to compute is not a state.

To know whether it was in `PAID`, the machine would have to compare
`amountInserted` against the price, and would have to reassign the state on
every `insertMoney` call. Anything derived on demand is a predicate, and a
stored predicate is a cache waiting to go stale. So "paid enough" stays an `if`
inside `dispense()`, deliberately separate from the state guard.

### Why the obvious version of this is a trap

The textbook move is `private MachineState state = IDLE;` beside
`private Slot selectedSlot;`. But those two fields encode the *same fact*:

```
state == IDLE          <->  selectedSlot == null
state == ITEM_SELECTED <->  selectedSlot != null
```

Two sources of truth that can disagree. Forget one assignment and the machine
believes it is idle while holding a selection. Worse, the enum has not removed a
single check — `if (selectedSlot == null)` simply becomes
`if (state != ITEM_SELECTED)`. Still seven. **The enum renames the branches; it
does not remove them.**

### Solution — derive the state, never store it

```
private MachineState state() {
    return selectedSlot == null ? IDLE : ITEM_SELECTED;
}
```

Nothing is ever assigned, so the two cannot disagree: the bug class is
structurally impossible rather than avoided by discipline. `selectedSlot` is
demoted to **payload** — data meaningful only while
`state() == ITEM_SELECTED`.

Every guard then goes through one helper:

```
private void requireState(MachineState expected, String message)
```

The `message` parameter is not decoration. A single generic
"Expected ITEM_SELECTED, Actual IDLE" is better for a stack trace and worse for
the person standing at the machine; passing the message per call site keeps
V1's user-readable errors while the comparison itself lives in one place.

Every method that cares about state must go through `state()` — including
`cancel()`. A method that still reads the field directly is the one that will
not notice when `state()` stops being derived.

### What the enum actually buys

Not fewer branches. Two other things:

1. **A vocabulary.** `ITEM_SELECTED` is a name usable in diagrams, logs and
   messages. `selectedSlot != null` is a fact that must be decoded.
2. **A seam.** Today `state()` derives. When `OUT_OF_SERVICE` arrives — a state
   nothing about `selectedSlot` can express — only `state()` changes, and not
   one guard that calls it. V1 had no such seam.

The price: derived state can only express what the data already implies, so
`OUT_OF_SERVICE` will force a hybrid (stored field plus derivation). V2a is the
right *next* step, not the end state.

### Class diagram

```mermaid
classDiagram
    class VendingMachine {
        -Inventory inventory
        -Slot selectedSlot
        -int amountInserted
        -state() MachineState
        -requireState(MachineState expected, String message) void
        +showItems() List~ItemView~
        +selectItem(String code) void
        +insertMoney(int amount) void
        +dispense() DispenseResult
        +cancel() int
    }

    class MachineState {
        <<enumeration>>
        IDLE
        ITEM_SELECTED
    }

    class Inventory {
        -Map~String, Slot~ slots
        +addSlot(Slot slot) void
        +getSlot(String code) Slot
        +allSlots() Collection~Slot~
    }

    class Slot {
        -String code
        -Item item
        -int quantity
        +isEmpty() boolean
        +reduceQuantity() void
    }

    VendingMachine *-- Inventory : composition
    Inventory *-- "1..*" Slot : composition
    VendingMachine --> Slot : selectedSlot (payload)
    VendingMachine ..> MachineState : derives
```

`VendingMachine ..> MachineState` is a **dependency**, not an association —
there is no field of that type. That missing field is the whole point of V2a.

### Flow

```
state() == IDLE
selectItem("A1")   -> requireState(IDLE, "An item is already selected.") ok
                   -> selectedSlot = A1        [state() now ITEM_SELECTED]
insertMoney(10)    -> requireState(ITEM_SELECTED, "No item selected.") ok
                   -> amountInserted = 10
dispense()         -> requireState(ITEM_SELECTED) ok    <- state guard
                   -> 10 < 25 -> "Insufficient funds"    <- condition, separate
cancel()           -> state() != IDLE -> refund 10      [state() now IDLE]
cancel()           -> state() == IDLE -> 0              (no-op, not an error)
```

`cancel()` is the one operation that *branches* on state rather than
*requiring* one — cancelling an idle machine is a no-op, not a user error.

### What changed, and why

| | V1 | V2a |
|---|---|---|
| State | inferred inline, per method | one `state()` method |
| Source of truth | the fields, read everywhere | derived, single definition |
| State comparison | 4 hand-rolled null checks | 1 place (`requireState`) |
| Vocabulary | none | `IDLE`, `ITEM_SELECTED` |
| A new non-derivable state | touches every guard | touches `state()` only |
| User-facing messages | per call site | per call site (preserved) |

Behaviour is unchanged — the demo output is identical to V1's. This version
bought structure, not features.

### Problems with V2a

**The rules are still invisible.** Ask "what operations are legal in
`ITEM_SELECTED`?" and there is no single place to look. The answer is spread
across four method bodies, as the first line of each. The machine's transition
table exists only in the reader's head, after reading all of them.

**Adding a state still edits every method.** `OUT_OF_SERVICE` means revisiting
each guard to decide what it does there. OCP is still violated, just more
tidily.

**`requireState` expresses only one legal state per operation.** As soon as an
operation is legal in two states, it needs a set, and the signature strains.

**Nothing prevents a forgotten guard.** A new sixth operation with no
`requireState` line compiles and runs happily. The guard is a convention, not a
structure.

---

## V2b — The transition table as data

### Problem

V2a named the states but left the **rules** invisible. Ask "what operations are
legal in `ITEM_SELECTED`?" and there is no place to look: you grep for
`requireState(ITEM_SELECTED)` and read what you find.

That grep returns a **wrong** answer, not merely a slow one:

| Operation | Legal in `ITEM_SELECTED`? | Visible to the grep? |
|---|---|---|
| `insertMoney` | yes | yes — `requireState(ITEM_SELECTED)` |
| `dispense` | yes | yes — `requireState(ITEM_SELECTED)` |
| `cancel` | yes | **no** — it *branches* on state instead of requiring one |
| `showItems` | yes | **no** — legal in every state, so it has no guard at all |

Operations legal everywhere, and operations that branch rather than require, are
structurally invisible. The transition table existed only in the reader's head
after reading every method body — and it dropped two of four entries.

### Solution — put legality on the state itself

A table needs two axes, so `Operation` joins `MachineState` as a first-class
type, and each state constant declares what it permits:

```
IDLE(EnumSet.of(SHOW_ITEMS, SELECT_ITEM, CANCEL)),
ITEM_SELECTED(EnumSet.of(SHOW_ITEMS, INSERT_MONEY, DISPENSE, CANCEL));
```

Question 21 is now answered by reading **one line**, in the type named after the
concept.

Why on the enum rather than a `Map` field in `VendingMachine`:

- a map inside `VendingMachine` would give that class both the purchase flow and
  the rulebook — the SRP complaint from V1, returning
- it is the stepping stone to V2c, where the state object owns *behaviour*. With
  legality already **on** the state, V2c replaces an `EnumSet` with real
  methods; with a map, V2c starts by dismantling the map
- a dedicated `TransitionRules` class is right only once rules come from
  configuration or vary per machine model (V5). Today it would wrap one map, so
  it does not earn its place — the same rule that deleted `Payment` in V1

The cost: `MachineState` now depends on `Operation`. Acceptable — they are two
halves of one concept, and the coupling is acyclic since `Operation` knows
nothing of states.

### What stays out of the table

Only rules decidable from the **state alone** belong in it. Anything that must
inspect **data** stays a condition in the method that owns the data:

| Rule | Knowable from state alone? | Lives in |
|---|---|---|
| cannot insert money before selecting | yes | the table |
| cannot select twice | yes | the table |
| item is sold out | no — reads `slot.quantity` | the method |
| insufficient funds | no — compares amount to price | the method |

So `selectItem` keeps two checks for two different reasons: a table lookup (may
I select at all?) and a data condition (is *this* slot stocked?). This is the
same state-vs-condition split that kept `PAID` out of the enum in V2a.

### Why the table cannot store `nextState`

A classic transition table maps `(state, operation) -> nextState`. This one maps
`state -> Set<Operation>` and deliberately answers only "is this move legal?".

`state()` is **derived from the data**, so the next state is a *consequence* of
what the operation does to `selectedSlot` — not something a table can dictate.
A table declaring `nextState` would re-create two authorities on state (the
table's claim and the data's reality), which is precisely the bug V2a was built
to make impossible.

### Class diagram

```mermaid
classDiagram
    class VendingMachine {
        -Inventory inventory
        -Slot selectedSlot
        -int amountInserted
        -state() MachineState
        +showItems() List~ItemView~
        +selectItem(String code) void
        +insertMoney(int amount) void
        +dispense() DispenseResult
        +cancel() int
    }

    class MachineState {
        <<enumeration>>
        IDLE
        ITEM_SELECTED
        -Set~Operation~ allowedOperations
        +isOperationAllowed(Operation op) boolean
        +requireOperation(Operation op, String message) void
    }

    class Operation {
        <<enumeration>>
        SHOW_ITEMS
        SELECT_ITEM
        INSERT_MONEY
        DISPENSE
        CANCEL
    }

    VendingMachine ..> MachineState : derives, then asks
    MachineState ..> Operation : permits
```

### Flow

```
state() == IDLE
selectItem("A1")   -> state().requireOperation(SELECT_ITEM) -> IDLE permits ok
                   -> slot.isEmpty()? no                     <- data condition
                   -> selectedSlot = A1       [state() now ITEM_SELECTED]
selectItem("B1")   -> state().requireOperation(SELECT_ITEM)
                   -> ITEM_SELECTED does not permit -> "An item is already selected."
insertMoney(25)    -> ITEM_SELECTED permits INSERT_MONEY ok
dispense()         -> ITEM_SELECTED permits DISPENSE ok
                   -> 25 >= 25 ok                            <- data condition
                   -> result, stock--, reset  [state() now IDLE]
```

### Two bugs this version shipped, and what they teach

**The guard asked a constant, not the machine.**
`MachineState.ITEM_SELECTED.requireOperation(INSERT_MONEY, ...)` reads
plausibly and is a tautology: a hardcoded constant always permits its own
operations. Every guard in the machine silently became a no-op, and the demo
sold a Water to someone who asked for a Coke. **A lookup table is only useful
when looked up with the state you are actually in** — `state()` from V2a is not
replaced by the table, it is what makes the table work.

**The wrong operation constant was passed.** `selectItem` asked about
`SHOW_ITEMS`, which every state permits, so selecting twice stayed legal even
after the receiver was fixed. Two independent bugs stacking in one guard line.

Both compiled, ran, and printed "ALLOWED (no error)" with exit code 0.

### Tests arrive here, not later

Those two regressions shipped in consecutive rounds, each a guard that compiled
and silently permitted an illegal sequence. `Main` *displays* breakage; it does
not *fail* on it. 43 JUnit tests now cover:

- the four `@Nested` groups in `VendingMachineTest` — happy path, illegal
  sequences, cancel, menu
- `MachineStateTest`, which asserts the **table itself**, without going through
  `VendingMachine`. When V2c replaces the table with state objects, these tests
  say whether the *rules* changed or only their implementation
- `SlotTest` and `InventoryTest` for stock invariants and lookup behaviour

Several assert more than "it throws", because these bugs were never about
whether an exception appeared:

- *"underpaying is rejected and the money is still held"* — then tops up and
  completes, so a guard that silently zeroed the balance would still fail
- *"selecting twice is rejected and the first selection survives"* — then buys
  the original item, so a guard that throws but overwrites the selection fails
- *"a rejected dispense does not consume stock"* — failure paths must not
  decrement
- *"every operation is permitted by at least one state"* — a table-level
  invariant, catching an `Operation` added to the enum but to no state's set

Reverting the one-token `SELECT_ITEM` fix turns the suite red on exactly the
right test. That is the property worth buying before V4 adds threads.

### What changed, and why

| | V2a | V2b |
|---|---|---|
| Legality rules | first line of each method | one line per state on the enum |
| "What is legal in X?" | read every method, miss two | read one line |
| Operations legal in all states | invisible (no guard) | explicit in the table |
| An operation legal in two states | needs a second `requireState` | already a `Set` |
| Rules mutable at runtime | n/a | no — unmodifiable `EnumSet` copy |
| Safety net | a demo that prints ALLOWED | 43 failing-on-regression tests |

### Problems with V2b

**A forgotten guard still compiles.** A new sixth operation with no
`requireOperation` line runs unguarded. The table is consulted by convention,
not by structure — `showItems` went unguarded for a round, and its table entries
were simply dead data.

**The operation constant can disagree with the method it guards.** Nothing ties
`SELECT_ITEM` to `selectItem()`; passing `SHOW_ITEMS` there compiled and
disabled the guard. The table knows *about* the operations but does not *own*
them.

**Behaviour is still centralised.** The table says what is *legal*; every method
still contains the behaviour for all states. `OUT_OF_SERVICE` means a new enum
constant *and* an edit to each method that must behave differently there. OCP is
narrower than in V2a but not satisfied.

**A state cannot carry its own data.** `ITEM_SELECTED` is a label with a
permission set. It cannot hold *which* slot is selected or *how much* has been
inserted — that payload still lives in `VendingMachine`, so state and its data
remain apart. This is the limitation V2c removes.

---
