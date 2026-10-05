# Vending Machine, Five Ways — interactive walkthrough

`index.html` is a single self-contained page that teaches the same V1 → V4
evolution as [`../VendingMachine.md`](../VendingMachine.md), but by letting you
press the buttons instead of reading about them.

Open it directly in a browser — no build step, no dependencies, no server:

```
xdg-open animation/index.html
```

It is also published as a private Artifact:
<https://claude.ai/artifact/TyUByxQ3kTnqFQoz1D5uH7>

## What it shows

A working machine (slots `A1`, `A2`, `B1`, a coin slot, a card reader, a display
and a tray) beside a state diagram that animates as you operate it.

**The point of the page:** switch version, then press the *same* buttons.

| Try this | V1 | V2a and later |
|---|---|---|
| `A1`, then DISPENSE with no money | hands over a free Coke, change −25 | "No item selected" / "Insufficient funds" |
| coins first, then DISPENSE | takes the money, then a NullPointerException | refused at the coin slot |
| `A1`, then `B1` | silently throws away the first choice and sells the wrong drink | "An item is already selected." |

In **V2b** each state node lists the operations it allows — the transition table
as data. In **V2c** the `ItemSelectedState` node shows the slot and the amount
*inside it*, which is the whole reason state objects replaced the enum. In
**V3** the card reader appears, with a gateway you can set to approve, decline
or time out.

Further down the page: a meter tracking where the rules live (7 scattered checks
→ 1 line → 0 written by hand), the patterns that were **rejected** and why, and
an animation of the V4 race condition — two threads both reading
`quantity = 1` before either writes `0`, so two cards get charged for one Coke.

## The class diagram

It is not on this page. It lives in `../VendingMachine.md`, where the rendered
`class-diagram.svg` is clickable: opening it gives a vector you can zoom as far as
you like, and the Mermaid source sits beside it in a collapsed block.

## Notes

- One file, no libraries. Fonts come from Google Fonts; everything else is
  inline.
- Works in light and dark, down to phone width, and honours
  `prefers-reduced-motion` by running the animations instantly.
- **V4 is operable.** Set the gateway to "times out" and the machine enters
  `PAYMENT_PENDING` and *stays* there: the claim is held on purpose, so cancel
  and select are refused, and a second swipe comes back `Busy`. Press
  "+31s" to let the next request take the stale claim over - the log then says
  what nobody has recorded.
- The race animation has two modes: V3's single read-then-write step, which
  charges two cards for one can, and V4's claim / act / commit, where the loser
  never reaches the gateway.
- The page stops at V4, because that is where the code stops. What is still missing
  is listed in `../VendingMachine.md` under **Open questions**, rather than shown as
  a version that does not exist.
