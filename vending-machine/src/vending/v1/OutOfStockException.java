package vending.v1;

public class OutOfStockException extends VendingMachineException {
    public OutOfStockException(String slotCode, String productName) {
        super("Slot " + slotCode + " (" + productName + ") is out of stock");
    }
}
