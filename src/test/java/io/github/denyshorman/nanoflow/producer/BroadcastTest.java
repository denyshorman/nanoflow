package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BroadcastTest {
    @Test
    void shouldBroadcastToMultipleSubscribers() throws InterruptedException {
        var broadcast = Flows.<Integer>broadcast();
        var ready = new CountDownLatch(2);
        var list1 = new ArrayList<Integer>();
        var list2 = new ArrayList<Integer>();

        var t1 = Thread.ofVirtual().start(() -> {
            try (var items = broadcast.open()) {
                ready.countDown();

                for (var item : items) {
                    list1.add(item);
                }
            }
        });

        var t2 = Thread.ofVirtual().start(() -> {
            try (var items = broadcast.open()) {
                ready.countDown();

                for (var item : items) {
                    list2.add(item);
                }
            }
        });

        assertTrue(ready.await(5, TimeUnit.SECONDS));

        broadcast.emit(1);
        broadcast.emit(1);
        broadcast.emit(2);
        broadcast.close();

        t1.join(5000);
        t2.join(5000);

        assertEquals(List.of(1, 1, 2), list1);
        assertEquals(List.of(1, 1, 2), list2);
    }

    @Test
    void shouldSupportMultipleProducersAndMultipleConsumers() throws InterruptedException {
        var broadcast = Flows.<String>broadcast(16);
        var ready = new CountDownLatch(2);
        var list1 = new ArrayList<String>();
        var list2 = new ArrayList<String>();

        var consumer1 = Thread.ofVirtual().start(() -> {
            try (var items = broadcast.open()) {
                ready.countDown();

                for (var item : items) {
                    list1.add(item);
                }
            }
        });

        var consumer2 = Thread.ofVirtual().start(() -> {
            try (var items = broadcast.open()) {
                ready.countDown();

                for (var item : items) {
                    list2.add(item);
                }
            }
        });

        assertTrue(ready.await(5, TimeUnit.SECONDS));

        var producer1 = Thread.ofVirtual().start(() -> {
            try {
                broadcast.emit("p1-a");
                broadcast.emit("p1-b");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        var producer2 = Thread.ofVirtual().start(() -> {
            try {
                broadcast.emit("p2-a");
                broadcast.emit("p2-b");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer1.join(5000);
        producer2.join(5000);
        broadcast.close();

        consumer1.join(5000);
        consumer2.join(5000);

        assertEquals(4, list1.size());
        assertEquals(list1, list2);
    }

    @Test
    void shouldDropEmissionsWhenNoSubscribersAreOpen() throws InterruptedException {
        var broadcast = Flows.<String>broadcast(10);

        broadcast.emit("dropped");

        var received = new CopyOnWriteArrayList<String>();
        var ready = new CountDownLatch(1);

        var t = Thread.ofVirtual().start(() -> {
            try (var items = broadcast.open()) {
                ready.countDown();

                for (var item : items) {
                    received.add(item);
                }
            }
        });

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        broadcast.emit("kept");
        broadcast.close();

        t.join(5000);
        assertEquals(List.of("kept"), received);
    }

    @Test
    void shouldContinueForRemainingSubscribersWhenOneClosesEarly() throws InterruptedException {
        var broadcast = Flows.<Integer>broadcast();
        var ready = new CountDownLatch(2);
        var list1 = new ArrayList<Integer>();
        var list2 = new ArrayList<Integer>();

        var t1 = Thread.ofVirtual().start(() -> {
            try (var items = broadcast.open()) {
                ready.countDown();

                for (var item : items) {
                    list1.add(item);
                    break;
                }
            }
        });

        var t2 = Thread.ofVirtual().start(() -> {
            try (var items = broadcast.open()) {
                ready.countDown();

                for (var item : items) {
                    list2.add(item);
                }
            }
        });

        assertTrue(ready.await(5, TimeUnit.SECONDS));

        broadcast.emit(1);
        broadcast.emit(2);
        broadcast.emit(3);
        broadcast.close();

        t1.join(5000);
        t2.join(5000);

        assertEquals(List.of(1), list1);
        assertEquals(List.of(1, 2, 3), list2);
    }

    @Test
    void shouldBufferElementsWithoutBlockingEmitter() throws InterruptedException {
        var broadcast = Flows.<Integer>broadcast(3);

        try (var items = broadcast.open()) {
            broadcast.emit(10);
            broadcast.emit(20);
            broadcast.emit(30);
            broadcast.close();

            var received = new ArrayList<Integer>();

            for (var item : items) {
                received.add(item);
            }

            assertEquals(List.of(10, 20, 30), received);
        }
    }

    @Test
    void shouldCompleteImmediatelyWhenOpenedAfterClose() {
        var broadcast = Flows.<Integer>broadcast();
        broadcast.close();

        assertEquals(List.of(), broadcast.toList());
        assertThrows(IllegalStateException.class, () -> broadcast.emit(1));
    }

    @Test
    @SuppressWarnings("resource")
    void shouldRejectNegativeBufferSize() {
        assertThrows(IllegalArgumentException.class, () -> Flows.broadcast(-1));
    }

    @Test
    void shouldEnforceSequenceAndIteratorContracts() {
        var broadcast = Flows.<String>broadcast(1);

        try (var items = broadcast.open()) {
            var iterator = items.iterator();
            assertThrows(NoSuchElementException.class, iterator::next);
        }

        var closedSequence = broadcast.open();
        closedSequence.close();

        assertThrows(IllegalStateException.class, closedSequence::iterator);
    }

    @Test
    @SuppressWarnings("resource")
    void shouldBlockEmitterWhenBufferIsFullUntilConsumerReads() throws InterruptedException {
        var broadcast = Flows.<Integer>broadcast(1);
        var secondEmitted = new CountDownLatch(1);

        try (var items = broadcast.open()) {
            broadcast.emit(1);

            var producer = Thread.ofVirtual().start(() -> {
                try {
                    broadcast.emit(2);
                    secondEmitted.countDown();
                    broadcast.close();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            assertFalse(secondEmitted.await(30, TimeUnit.MILLISECONDS));

            var iterator = items.iterator();
            assertTrue(iterator.hasNext());
            assertEquals(1, iterator.next());

            assertTrue(secondEmitted.await(5, TimeUnit.SECONDS));
            assertTrue(iterator.hasNext());
            assertEquals(2, iterator.next());
            assertFalse(iterator.hasNext());

            producer.join(5000);
        }
    }

    @Test
    void shouldUnblockEmitterWhenBlockedSubscriberCloses() throws InterruptedException {
        var broadcast = Flows.<Integer>broadcast(1);
        var secondEmitted = new CountDownLatch(1);
        var sequence = broadcast.open();

        broadcast.emit(1);

        var producer = Thread.ofVirtual().start(() -> {
            try {
                broadcast.emit(2);
                secondEmitted.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertFalse(secondEmitted.await(30, TimeUnit.MILLISECONDS));
        sequence.close();

        assertTrue(secondEmitted.await(5, TimeUnit.SECONDS));
        producer.join(5000);
        broadcast.close();
    }

    @Test
    void shouldStopWhenConsumerThreadIsInterrupted() throws InterruptedException {
        var broadcast = Flows.<Integer>broadcast();
        var ready = new CountDownLatch(1);
        var interruptedFlag = new CountDownLatch(1);

        var consumer = Thread.ofVirtual().start(() -> {
            assertThrows(InterruptedException.class, () -> {
                try (var items = broadcast.open()) {
                    ready.countDown();

                    for (var ignored : items) {
                        fail("Should not receive any elements");
                    }
                }
            });

            if (Thread.currentThread().isInterrupted()) {
                interruptedFlag.countDown();
            }
        });

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        Thread.sleep(20);
        consumer.interrupt();

        assertTrue(interruptedFlag.await(5, TimeUnit.SECONDS));
        consumer.join(5000);
        broadcast.close();
    }

    @Test
    void shouldInterruptProducerBlockedInEmit() throws InterruptedException {
        try (var broadcast = Flows.<Integer>broadcast(1); var ignored = broadcast.open()) {
            var interrupted = new CountDownLatch(1);
            broadcast.emit(1);

            var producer = Thread.ofVirtual().start(() -> {
                assertThrows(InterruptedException.class, () -> broadcast.emit(2));
                interrupted.countDown();
            });

            Thread.sleep(20);
            producer.interrupt();

            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            producer.join(5000);
        }
    }

    @Test
    void shouldComposeWithFlowOperators() throws InterruptedException {
        var broadcast = Flows.<Integer>broadcast(10);
        var ready = new CountDownLatch(1);
        var result = new CopyOnWriteArrayList<Integer>();

        var consumer = Thread.ofVirtual().start(() -> {
            var list = broadcast
                    .filter(n -> {
                        ready.countDown();
                        return n % 2 == 0;
                    })
                    .map(n -> n * 10)
                    .take(2)
                    .toList();

            result.addAll(list);
        });

        Thread.sleep(20);
        broadcast.emit(1);
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        broadcast.emit(2);
        broadcast.emit(3);
        broadcast.emit(4);

        consumer.join(5000);
        broadcast.close();
        assertEquals(List.of(20, 40), result);
    }
}
