package vending.v1;

public class InsufficientBalanceException extends VendingMachineException {

    private final Money price;
    private final Money inserted;

    public InsufficientBalanceException(Money price, Money inserted) {
        super("Price is " + price + " but only " + inserted + " inserted");
        this.price = price;
        this.inserted = inserted;
    }

    public Money shortfall() { return price.subtract(inserted); }
}
