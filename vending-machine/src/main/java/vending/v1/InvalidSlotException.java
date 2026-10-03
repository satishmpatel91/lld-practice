package vending.v1;

public class InvalidSlotException extends VendingMachineException {
    public InvalidSlotException(String code) {
        super("No such slot: " + code);
    }
}
