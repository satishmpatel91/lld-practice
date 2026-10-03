package vending.v1;

public class VendingMachineDemo {

    public static void main(String[] args) {
        Inventory inventory = new Inventory();
        inventory.addSlot(new ProductSlot("A1", new Product("P1", "Coke",   Money.of("25")), 10, 2));
        inventory.addSlot(new ProductSlot("A2", new Product("P2", "Chips",  Money.of("20")), 10, 1));
        inventory.addSlot(new ProductSlot("A3", new Product("P3", "Water",  Money.of("15")), 10, 0));

        VendingMachine machine = new VendingMachine(inventory);

        System.out.println("=== 1. Display available products ===");
        machine.availableProducts().forEach(s -> System.out.println("  " + s));

        System.out.println("\n=== 2. Happy path: buy Coke with Rs.50 ===");
        machine.selectProduct("A1");
        System.out.println("  balance after Rs.20: " + machine.insertMoney(Money.of("20")));
        System.out.println("  balance after Rs.30: " + machine.insertMoney(Money.of("30")));
        System.out.println("  " + machine.dispense());
        System.out.println("  balance is reset to: " + machine.insertedAmount());

        System.out.println("\n=== 3. Insufficient balance ===");
        machine.selectProduct("A1");
        machine.insertMoney(Money.of("10"));
        try {
            machine.dispense();
        } catch (InsufficientBalanceException e) {
            System.out.println("  " + e.getMessage() + " | short by " + e.shortfall());
        }

        System.out.println("\n=== 4. Cancel -> refund ===");
        System.out.println("  refunded: " + machine.cancel());
        System.out.println("  selection cleared: " + machine.selectedSlot().isEmpty());

        System.out.println("\n=== 5. Out of stock on selection ===");
        try {
            machine.selectProduct("A3");
        } catch (OutOfStockException e) {
            System.out.println("  " + e.getMessage());
        }

        System.out.println("\n=== 6. Unknown slot ===");
        try {
            machine.selectProduct("Z9");
        } catch (InvalidSlotException e) {
            System.out.println("  " + e.getMessage());
        }

        System.out.println("\n=== 7. Dispense without selecting ===");
        try {
            machine.dispense();
        } catch (NoProductSelectedException e) {
            System.out.println("  " + e.getMessage());
        }

        System.out.println("\n=== 8. Exact change, last unit of A2 ===");
        machine.selectProduct("A2");
        machine.insertMoney(Money.of("20"));
        System.out.println("  " + machine.dispense());
        machine.availableProducts().forEach(s -> System.out.println("  still available: " + s));
    }
}
