package vending.v1;

public class NoProductSelectedException extends VendingMachineException {
    public NoProductSelectedException() {
        super("No product selected");
    }
}
