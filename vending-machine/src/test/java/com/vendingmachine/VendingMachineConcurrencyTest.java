package com.vendingmachine;

import com.vendingmachine.payment.Card;
import com.vendingmachine.payment.FakePaymentGateway;
import com.vendingmachine.payment.FakePaymentGateway.Behaviour;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The properties that only concurrency can break. Every test releases its
 * threads from one latch, so they genuinely overlap rather than queueing.
 */
class VendingMachineConcurrencyTest {

    private static final int THREADS = 16;
    private static final int COKE_PRICE = 25;
    private static final Card CARD = new Card("tok_visa_4242");

    private Inventory oneCoke() {
        Inventory inventory = new Inventory();
        inventory.addSlot(new Slot("A1", new Item("Coke", COKE_PRICE), 1));
        return inventory;
    }

    /** Runs every task at once and returns the results, failures included as null. */
    private <T> List<T> allAtOnce(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    try {
                        return task.call();
                    } catch (RuntimeException e) {
                        return null;
                    }
                }));
            }
            go.countDown();
            List<T> out = new java.util.ArrayList<>();
            for (Future<T> f : futures) {
                out.add(f.get(10, TimeUnit.SECONDS));
            }
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("only one thread can buy the last item by card, and the card is charged once")
    void onlyOneThreadCanBuyTheLastItemByCard() throws Exception {
        Inventory inventory = oneCoke();
        FakePaymentGateway gateway = new FakePaymentGateway(Behaviour.APPROVE);
        VendingMachine machine = new VendingMachine(inventory, gateway);
        machine.selectItem("A1");

        List<PurchaseResult> results = allAtOnce(THREADS, () -> machine.swipeCard(CARD));

        long dispensed = results.stream().filter(r -> r instanceof PurchaseResult.Dispensed).count();
        long busy = results.stream().filter(r -> r instanceof PurchaseResult.Busy).count();

        assertEquals(1, dispensed, "exactly one customer may get the last Coke");
        assertEquals(THREADS - 1, busy, "everyone else is told the machine is busy");
        assertEquals(1, gateway.chargeCount(), "and exactly one card was charged");
        assertEquals(0, inventory.getSlot("A1").getQuantity(), "stock must never go negative");
    }

    @Test
    @DisplayName("concurrent cash dispenses hand over one item, not several")
    void concurrentCashDispensesHandOverOneItem() throws Exception {
        Inventory inventory = oneCoke();
        VendingMachine machine = new VendingMachine(inventory);
        machine.selectItem("A1");
        machine.insertMoney(COKE_PRICE);

        List<DispenseResult> results = allAtOnce(THREADS, machine::dispense);

        long served = results.stream().filter(r -> r != null).count();
        assertEquals(1, served, "a double-pressed button must not dispense twice");
        assertEquals(0, inventory.getSlot("A1").getQuantity());
    }

    @Test
    @DisplayName("no coin is lost when money is inserted concurrently")
    void noCoinIsLostWhenInsertingConcurrently() throws Exception {
        Inventory inventory = oneCoke();
        VendingMachine machine = new VendingMachine(inventory);
        machine.selectItem("A1");

        allAtOnce(THREADS, () -> {
            machine.insertMoney(10);
            return null;
        });

        // every insert must be accounted for: 16 x 10, less the 25 price
        assertEquals(THREADS * 10 - COKE_PRICE, machine.dispense().change(),
                "a lost update here would quietly keep the customer's money");
    }

    @Test
    @DisplayName("a crowded machine sells exactly its stock, no more and no less")
    void sellsExactlyItsStock() throws Exception {
        int stock = 6;
        Inventory inventory = new Inventory();
        inventory.addSlot(new Slot("A1", new Item("Coke", COKE_PRICE), stock));
        VendingMachine machine = new VendingMachine(inventory);
        AtomicInteger sold = new AtomicInteger();

        allAtOnce(THREADS, () -> {
            for (int attempt = 0; attempt < 40; attempt++) {
                try {
                    machine.selectItem("A1");
                    machine.insertMoney(COKE_PRICE);
                    machine.dispense();
                    sold.incrementAndGet();
                } catch (RuntimeException e) {
                    /* lost a race, or the slot is empty now - both are expected */
                }
            }
            return null;
        });

        assertEquals(stock, sold.get(), "every can is sold exactly once");
        assertEquals(0, inventory.getSlot("A1").getQuantity());
        assertTrue(inventory.getSlot("A1").isEmpty());
    }
}
