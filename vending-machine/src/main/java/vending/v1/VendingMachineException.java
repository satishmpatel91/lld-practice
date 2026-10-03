package vending.v1;

/** Base type so a caller can catch all machine-rule violations in one clause. */
public class VendingMachineException extends RuntimeException {
    public VendingMachineException(String message) {
        super(message);
    }
}
