package com.vendingmachine;

import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.PaymentGateway;
import com.vendingmachine.payment.PaymentGatewayException;
import com.vendingmachine.payment.PaymentResult;

/** A user walking up to the machine. Drives the cash and card flows end to end. */
public class Main {

    public static void main(String[] args) {
        Inventory inventory = new Inventory();
        inventory.addSlot(new Slot("A1", new Item("Coke", 25), 3));
        inventory.addSlot(new Slot("A2", new Item("Chips", 20), 0));
        inventory.addSlot(new Slot("B1", new Item("Water", 15), 2));

        // PaymentGateway has a single method, so a demo gateway is a lambda.
        // Nothing fake ships in production code; the scripted fake lives in src/test.
        PaymentGateway approvingGateway = (card, amount) -> new PaymentResult.Approved(0, "DEMO-AUTH");
        VendingMachine machine = new VendingMachine(inventory, approvingGateway);

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

        System.out.println("--- card payments ---");
        Card card = new Card("tok_visa_4242");
        machine.selectItem("B1");
        System.out.println("  approved -> " + describe(machine.swipeCard(card)));

        VendingMachine declining = new VendingMachine(inventory,
                (c, amount) -> new PaymentResult.Declined("Card declined by issuer."));
        declining.selectItem("A1");
        System.out.println("  declined -> " + describe(declining.swipeCard(card)));
        System.out.println("  selection survives a decline, so cash still works:");
        declining.insertMoney(25);
        System.out.println("    " + declining.dispense().item().getName() + " paid in cash");

        VendingMachine cashOnly = new VendingMachine(inventory);
        cashOnly.selectItem("A1");
        System.out.println("  no card reader -> " + describe(cashOnly.swipeCard(card)));
        System.out.println("  refund = " + cashOnly.cancel());

        VendingMachine broken = new VendingMachine(inventory, (c, amount) -> {
            throw new PaymentGatewayException("Gateway unreachable.");
        });
        broken.selectItem("A1");
        attempt("gateway failure (outcome unknown)", () -> broken.swipeCard(card));
        System.out.println("  state did not advance, refund = " + broken.cancel());

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

    private static String describe(PurchaseResult result) {
        return switch (result) {
            case PurchaseResult.Dispensed d ->
                    "got " + d.item().getName() + ", change " + d.change() + ", ref " + d.reference();
            case PurchaseResult.Declined d -> "declined: " + d.reason();
        };
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
