package com.vendingmachine;

/** A user walking up to the machine. Drives the V1 flow end to end. */
public class Main {

    public static void main(String[] args) {
        Inventory inventory = new Inventory();
        inventory.addSlot(new Slot("A1", new Item("Coke", 25), 3));
        inventory.addSlot(new Slot("A2", new Item("Chips", 20), 0));
        inventory.addSlot(new Slot("B1", new Item("Water", 15), 2));

        VendingMachine machine = new VendingMachine(inventory);

        System.out.println("--- what the user sees ---");
        printMenu(machine);

        System.out.println("--- happy path: Coke 25, pay 50 ---");
        machine.selectItem("A1");
        machine.insertMoney(50);
        DispenseResult dispenseResult = machine.dispense();
        System.out.println(String.format("Here is your %s and ₹%d", dispenseResult.item().getName(), dispenseResult.change()));

        System.out.println("--- happy path: Water 15, pay 20 ---");
        machine.selectItem("B1");
        machine.insertMoney(20);
        DispenseResult waterResult = machine.dispense();
        System.out.println(String.format("Here is your %s and ₹%d", waterResult.item().getName(), waterResult.change()));

        System.out.println("--- cancel ---");
        machine.selectItem("A1");
        machine.insertMoney(10);
        System.out.println("  refund = " + machine.cancel());
        System.out.println("  cancel on idle machine = " + machine.cancel());

        System.out.println("--- illegal sequences ---");
        attempt("dispense, nothing selected", machine::dispense);
        attempt("select sold-out A2", () -> machine.selectItem("A2"));
        attempt("select unknown Z9", () -> machine.selectItem("Z9"));
        attempt("insert money, nothing selected", () -> machine.insertMoney(10));

        machine.selectItem("A1");
        attempt("select again while selected", () -> machine.selectItem("B1"));
        attempt("insert negative", () -> machine.insertMoney(-100));
        attempt("underpay 10 for a 25 item", () -> { machine.insertMoney(10); machine.dispense(); });
        System.out.println("  refund = " + machine.cancel());

        System.out.println("--- drain A1 to sold out ---");
        int buy = 0;
        while (true) {
            try {
                machine.selectItem("A1");
            } catch (RuntimeException e) {
                System.out.println("  next buy -> " + e.getMessage());
                break;
            }
            machine.insertMoney(25);
            DispenseResult result = machine.dispense();
            System.out.println("  buy " + (++buy) + ": " + result.item().getName());
        }
        System.out.println("--- menu after A1 sold out ---");
        printMenu(machine);
    }

    private static void printMenu(VendingMachine machine) {
        machine.showItems().forEach(view -> System.out.printf("  %-3s %-6s %3d  %s%n",
                view.slotCode(), view.name(), view.price(),
                view.available() ? "" : "SOLD OUT"));
    }

    private static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  " + label + " -> ALLOWED (no error)");
        } catch (RuntimeException e) {
            System.out.println("  " + label + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
