package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StateTest {
    record User(int id, String name) {
    }

    @Test
    void shouldReturnInitialValue() {
        var state = Flows.state("initial");

        assertEquals("initial", state.get());
        assertEquals("initial", state.first());
    }

    @Test
    void shouldUpdateAndGetValues() {
        var state = Flows.state(10);

        state.set(20);
        assertEquals(20, state.get());

        assertTrue(state.compareAndSet(20, 40));
        assertEquals(40, state.get());

        assertFalse(state.compareAndSet(20, 50));
        assertEquals(40, state.get());

        assertEquals(45, state.update(n -> n + 5));
        assertEquals(45, state.get());
    }

    @Test
    void shouldEmitInitialAndSubsequentDistinctChanges() throws InterruptedException {
        var state = Flows.state(1);
        var received = new CopyOnWriteArrayList<Integer>();
        var firstReceived = new CountDownLatch(1);
        var secondReceived = new CountDownLatch(1);
        var thirdReceived = new CountDownLatch(1);

        var consumer = Thread.ofVirtual().start(() -> {
            try (var items = state.open()) {
                for (var item : items) {
                    received.add(item);

                    if (received.size() == 1) {
                        firstReceived.countDown();
                    } else if (received.size() == 2) {
                        secondReceived.countDown();
                    } else if (received.size() == 3) {
                        thirdReceived.countDown();
                        break;
                    }
                }
            }
        });

        assertTrue(firstReceived.await(5, TimeUnit.SECONDS));
        state.set(1); // Duplicate, should be ignored
        state.set(2);
        assertTrue(secondReceived.await(5, TimeUnit.SECONDS));
        state.set(2); // Duplicate, should be ignored
        state.set(3);
        assertTrue(thirdReceived.await(5, TimeUnit.SECONDS));

        consumer.join(5000);
        assertEquals(List.of(1, 2, 3), received);
    }

    @Test
    void shouldConflateRapidUpdatesForSlowConsumer() throws InterruptedException {
        var state = Flows.state(0);
        var received = new ArrayList<Integer>();
        var initialRead = new CountDownLatch(1);
        var updatesDone = new CountDownLatch(1);

        var consumer = Thread.ofVirtual().start(() -> {
            try (var items = state.open()) {
                for (var item : items) {
                    received.add(item);

                    if (item == 0) {
                        initialRead.countDown();

                        try {
                            updatesDone.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else if (item == 100) {
                        break;
                    }
                }
            }
        });

        assertTrue(initialRead.await(5, TimeUnit.SECONDS));

        for (int i = 1; i <= 100; i++) {
            state.set(i);
        }

        updatesDone.countDown();

        consumer.join(5000);
        assertEquals(List.of(0, 100), received);
    }

    @Test
    void shouldSupportMultipleConcurrentSubscribers() throws InterruptedException {
        var state = Flows.state("v1");
        var sub1Ready = new CountDownLatch(1);
        var sub2Ready = new CountDownLatch(1);
        var sub2ReceivedV2 = new CountDownLatch(1);
        var list1 = new CopyOnWriteArrayList<String>();
        var list2 = new CopyOnWriteArrayList<String>();

        var t1 = Thread.ofVirtual().start(() -> {
            try (var items = state.open()) {
                for (var item : items) {
                    list1.add(item);

                    if (item.equals("v1")) sub1Ready.countDown();
                    if (item.equals("v2")) break; // Sub 1 unsubscribes early
                }
            }
        });

        var t2 = Thread.ofVirtual().start(() -> {
            try (var items = state.open()) {
                for (var item : items) {
                    list2.add(item);

                    if (item.equals("v1")) sub2Ready.countDown();
                    if (item.equals("v2")) sub2ReceivedV2.countDown();
                    if (item.equals("v3")) break;
                }
            }
        });

        assertTrue(sub1Ready.await(5, TimeUnit.SECONDS));
        assertTrue(sub2Ready.await(5, TimeUnit.SECONDS));

        state.set("v2");
        t1.join(5000);
        assertTrue(sub2ReceivedV2.await(5, TimeUnit.SECONDS));

        state.set("v3");
        t2.join(5000);

        assertEquals(List.of("v1", "v2"), list1);
        assertEquals(List.of("v1", "v2", "v3"), list2);
    }

    @Test
    void shouldHandleConcurrentUpdatesSafely() {
        var state = Flows.state(0);
        var threads = 100;
        var incrementsPerThread = 100;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var i = 0; i < threads; i++) {
                executor.submit(() -> {
                    for (var j = 0; j < incrementsPerThread; j++) {
                        state.update(n -> n + 1);
                    }
                });
            }
        }

        assertEquals(threads * incrementsPerThread, state.get());
    }

    @Test
    void shouldUseValueEqualityForRecordsInSetAndCompareAndSet() {
        var state = Flows.state(new User(1, "Alice"));

        assertTrue(state.compareAndSet(new User(1, "Alice"), new User(1, "Bob")));
        assertEquals(new User(1, "Bob"), state.get());
    }

    @Test
    void shouldNotEmitWhenValueChangesAndRevertsWhileConsumerIsBusy() throws InterruptedException {
        var state = Flows.state("A");
        var received = new ArrayList<String>();
        var firstRead = new CountDownLatch(1);
        var abaDone = new CountDownLatch(1);

        var consumer = Thread.ofVirtual().start(() -> {
            try (var items = state.open()) {
                for (var item : items) {
                    received.add(item);

                    if (item.equals("A")) {
                        firstRead.countDown();

                        try {
                            abaDone.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else if (item.equals("C")) {
                        break;
                    }
                }
            }
        });

        assertTrue(firstRead.await(5, TimeUnit.SECONDS));

        state.set("B");
        state.set("A");
        abaDone.countDown();

        Thread.sleep(20);
        state.set("C");

        consumer.join(5000);
        assertEquals(List.of("A", "C"), received);
    }

    @Test
    void shouldComposeWithFlowOperators() throws InterruptedException {
        var state = Flows.state(1);
        var started = new CountDownLatch(1);
        var result = new CopyOnWriteArrayList<Integer>();

        var consumer = Thread.ofVirtual().start(() -> {
            var list = state
                    .filter(n -> {
                        started.countDown();
                        return n % 2 == 0;
                    })
                    .map(n -> n * 10)
                    .take(2)
                    .toList();

            result.addAll(list);
        });

        assertTrue(started.await(5, TimeUnit.SECONDS));
        state.set(2);
        Thread.sleep(20);
        state.set(3);
        Thread.sleep(20);
        state.set(4);

        consumer.join(5000);
        assertEquals(List.of(20, 40), result);
    }

    @Test
    void shouldStopWhenConsumerThreadIsInterrupted() throws InterruptedException {
        var state = Flows.state(1);
        var initialRead = new CountDownLatch(1);
        var interruptedFlag = new CountDownLatch(1);

        var consumer = Thread.ofVirtual().start(() -> {
            assertThrows(InterruptedException.class, () -> {
                try (var items = state.open()) {
                    for (var item : items) {
                        if (item == 1) {
                            initialRead.countDown();
                        }
                    }
                }
            });

            if (Thread.currentThread().isInterrupted()) {
                interruptedFlag.countDown();
            }
        });

        assertTrue(initialRead.await(5, TimeUnit.SECONDS));
        Thread.sleep(20);
        consumer.interrupt();

        assertTrue(interruptedFlag.await(5, TimeUnit.SECONDS));
        consumer.join(5000);
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void shouldEnforceSequenceAndIteratorContracts() {
        var state = Flows.state("x");

        try (var items = state.open()) {
            var iterator = items.iterator();

            assertThrows(NoSuchElementException.class, iterator::next);
            assertTrue(iterator.hasNext());
            assertTrue(iterator.hasNext());
            assertEquals("x", iterator.next());
        }

        var closedSequence = state.open();
        closedSequence.close();

        assertThrows(IllegalStateException.class, closedSequence::iterator);
    }
}
