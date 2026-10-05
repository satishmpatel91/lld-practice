package com.vendingmachine;

import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.PaymentGateway;
import com.vendingmachine.payment.PaymentGatewayException;
import com.vendingmachine.payment.PaymentResult;

import java.util.List;

/**
 * A customer walking up to the machine. Drives the cash and card flows end to end.
 *
 * <p>Written without lambdas on purpose: the stand-in gateways are small named
 * classes, so a reader can see what the interface asks for, and each illegal
 * sequence calls a named helper rather than passing a block of code around.
 */
public class Main {

    private static final Card CARD = new Card("tok_visa_4242");

    public static void main(String[] args) {
        Inventory inventory = new Inventory();
        inventory.addSlot(new Slot("A1", new Item("Coke", 25), 3));
        inventory.addSlot(new Slot("A2", new Item("Chips", 20), 0));
        inventory.addSlot(new Slot("B1", new Item("Water", 15), 2));

        VendingMachine machine = new VendingMachine(inventory, new ApprovingGateway());

        System.out.println("--- what the user sees ---");
        printMenu(machine);

        System.out.println("--- happy path: Coke 25, pay 50 ---");
        machine.selectItem("A1");
        machine.insertMoney(50);
        DispenseResult dispenseResult = machine.dispense();
        System.out.printf("Here is your %s and ₹%d%n",
                dispenseResult.item().getName(), dispenseResult.change());

        System.out.println("--- happy path: Water 15, pay 20 ---");
        machine.selectItem("B1");
        machine.insertMoney(20);
        DispenseResult waterResult = machine.dispense();
        System.out.printf("Here is your %s and ₹%d%n",
                waterResult.item().getName(), waterResult.change());

        System.out.println("--- cancel ---");
        machine.selectItem("A1");
        machine.insertMoney(10);
        System.out.println("  refund = " + machine.cancel());
        System.out.println("  cancel on idle machine = " + machine.cancel());

        System.out.println("--- illegal sequences ---");
        attemptDispense(machine, "dispense, nothing selected");
        attemptSelect(machine, "A2", "select sold-out A2");
        attemptSelect(machine, "Z9", "select unknown Z9");
        attemptInsert(machine, 10, "insert money, nothing selected");

        machine.selectItem("A1");
        attemptSelect(machine, "B1", "select again while selected");
        attemptInsert(machine, -100, "insert negative");
        machine.insertMoney(10);
        attemptDispense(machine, "underpay 10 for a 25 item");
        System.out.println("  refund = " + machine.cancel());

        System.out.println("--- card payments ---");
        machine.selectItem("B1");
        System.out.println("  approved -> " + describe(machine.swipeCard(CARD)));

        VendingMachine declining = new VendingMachine(inventory, new DecliningGateway());
        declining.selectItem("A1");
        System.out.println("  declined -> " + describe(declining.swipeCard(CARD)));
        System.out.println("  selection survives a decline, so cash still works:");
        declining.insertMoney(25);
        System.out.println("    " + declining.dispense().item().getName() + " paid in cash");

        VendingMachine cashOnly = new VendingMachine(inventory);
        cashOnly.selectItem("A1");
        System.out.println("  no card reader -> " + describe(cashOnly.swipeCard(CARD)));
        System.out.println("  refund = " + cashOnly.cancel());

        VendingMachine broken = new VendingMachine(inventory, new HangingGateway());
        broken.selectItem("A1");
        attemptSwipe(broken, "gateway failure (outcome unknown)");
        System.out.println("  the claim is held on purpose, so nobody can buy an item we may have paid for:");
        attemptCancel(broken, "  cancel while the charge is unresolved");
        attemptSelect(broken, "B1", "  select something else");
        System.out.println("  after the 30s timeout a later request takes the claim over (see the tests,");
        System.out.println("  which move an injected Clock instead of waiting)");

        System.out.println("--- drain A1 to sold out ---");
        int bought = 0;
        while (true) {
            try {
                machine.selectItem("A1");
            } catch (RuntimeException e) {
                System.out.println("  next buy -> " + e.getMessage());
                break;
            }
            machine.insertMoney(25);
            bought += 1;
            System.out.println("  buy " + bought + ": " + machine.dispense().item().getName());
        }

        System.out.println("--- menu after A1 sold out ---");
        printMenu(machine);
    }

    /* ---------- stand-in gateways: named classes rather than inline lambdas ---------- */

    private static final class ApprovingGateway implements PaymentGateway {
        @Override
        public PaymentResult charge(Card card, int amount, String idempotencyKey) {
            return new PaymentResult.Approved(0, "DEMO-AUTH");
        }
    }

    private static final class DecliningGateway implements PaymentGateway {
        @Override
        public PaymentResult charge(Card card, int amount, String idempotencyKey) {
            return new PaymentResult.Declined("Card declined by issuer.");
        }
    }

    /** Never answers, which is the case nobody can interpret. */
    private static final class HangingGateway implements PaymentGateway {
        @Override
        public PaymentResult charge(Card card, int amount, String idempotencyKey) {
            throw new PaymentGatewayException("Gateway unreachable.");
        }
    }

    /* ---------- output ---------- */

    private static String describe(PurchaseResult result) {
        return switch (result) {
            case PurchaseResult.Dispensed d ->
                    "got " + d.item().getName() + ", change " + d.change() + ", ref " + d.reference();
            case PurchaseResult.Declined d -> "declined: " + d.reason();
            case PurchaseResult.Busy b -> "busy: " + b.reason();
        };
    }

    private static void printMenu(VendingMachine machine) {
        List<ItemView> menu = machine.showItems();
        for (ItemView view : menu) {
            System.out.printf("  %-3s %-6s %3d  %s%n",
                    view.slotCode(), view.name(), view.price(),
                    view.available() ? "" : "SOLD OUT");
        }
    }

    /* ---------- one helper per operation we expect to be refused ---------- */

    private static void attemptSelect(VendingMachine machine, String code, String label) {
        try {
            machine.selectItem(code);
            allowed(label);
        } catch (RuntimeException e) {
            refused(label, e);
        }
    }

    private static void attemptInsert(VendingMachine machine, int amount, String label) {
        try {
            machine.insertMoney(amount);
            allowed(label);
        } catch (RuntimeException e) {
            refused(label, e);
        }
    }

    private static void attemptDispense(VendingMachine machine, String label) {
        try {
            machine.dispense();
            allowed(label);
        } catch (RuntimeException e) {
            refused(label, e);
        }
    }

    private static void attemptSwipe(VendingMachine machine, String label) {
        try {
            machine.swipeCard(CARD);
            allowed(label);
        } catch (RuntimeException e) {
            refused(label, e);
        }
    }

    private static void attemptCancel(VendingMachine machine, String label) {
        try {
            machine.cancel();
            allowed(label);
        } catch (RuntimeException e) {
            refused(label, e);
        }
    }

    private static void allowed(String label) {
        System.out.println("  " + label + " -> ALLOWED (no error)");
    }

    private static void refused(String label, RuntimeException e) {
        System.out.println("  " + label + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
