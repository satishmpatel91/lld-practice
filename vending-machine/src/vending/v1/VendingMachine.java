package vending.v1;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates one customer transaction: selection -> money -> dispense/cancel.
 *
 * Owns the CURRENT TRANSACTION (which slot is selected, how much is inserted).
 * Does NOT own stock counts (Inventory/ProductSlot do) and does NOT own prices
 * (Product does). It coordinates; it does not compute on others' behalf.
 */
public class VendingMachine {

    private final Inventory inventory;

    // --- current transaction state ---
    private ProductSlot selectedSlot;
    private Money insertedAmount = Money.ZERO;

    public VendingMachine(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /** Display: return data, not formatted text. Formatting is the caller's job. */
    public List<ProductSlot> availableProducts() {
        return inventory.availableSlots();
    }

    public void selectProduct(String slotCode) {
        ProductSlot slot = inventory.slotOf(slotCode);
        if (!slot.hasStock()) {
            throw new OutOfStockException(slot.code(), slot.product().name());
        }
        this.selectedSlot = slot;
    }

    /** Accepts money and returns the running balance. */
    public Money insertMoney(Money money) {
        Objects.requireNonNull(money, "money");
        if (money.isZero()) {
            throw new IllegalArgumentException("Inserted amount must be greater than zero");
        }
        this.insertedAmount = this.insertedAmount.add(money);
        return this.insertedAmount;
    }

    public DispenseResult dispense() {
        if (selectedSlot == null) {
            throw new NoProductSelectedException();
        }
        ProductSlot slot = selectedSlot;                  // capture before reset()
        Money price = slot.product().price();

        if (!slot.hasStock()) {                           // may have emptied since selection
            throw new OutOfStockException(slot.code(), slot.product().name());
        }
        if (insertedAmount.isLessThan(price)) {
            throw new InsufficientBalanceException(price, insertedAmount);
        }

        slot.dispenseOne();
        Money change = insertedAmount.subtract(price);
        Product product = slot.product();
        resetTransaction();
        return new DispenseResult(product, change);
    }

    /** Abort: refund everything inserted so far and forget the selection. */
    public Money cancel() {
        Money refund = insertedAmount;
        resetTransaction();
        return refund;
    }

    public Money insertedAmount() {
        return insertedAmount;
    }

    public Optional<ProductSlot> selectedSlot() {
        return Optional.ofNullable(selectedSlot);
    }

    private void resetTransaction() {
        this.selectedSlot = null;
        this.insertedAmount = Money.ZERO;
    }
}
