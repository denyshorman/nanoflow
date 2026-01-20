package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentFlowTest {
    @Test
    void shouldCollectAllValuesWhenEmittedSequentially() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        });

        var list = new LinkedList<Integer>();
        flow.collect(list::add);
        assertEquals(3, list.size());
        assertTrue(list.containsAll(List.of(1, 2, 3)));
    }

    @Test
    void shouldCollectAllValuesWhenEmittedConcurrently() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var i = 0; i < 1000; i++) {
                    final var value = i;
                    executor.submit(() -> emitter.emit(value));
                }
            }
        });

        var list = new LinkedList<Integer>();
        flow.collect(list::add);
        assertEquals(1000, list.size());
    }

    @Test
    void shouldPropagateCheckedExceptionFromEmitter() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            throw new IOException("test");
        });

        var exception = assertThrows(IOException.class, () -> flow.collect(System.out::println));
        assertEquals("test", exception.getMessage());
    }

    @Test
    @SuppressWarnings("CatchMayIgnoreException")
    void shouldStopEmittingWhenCollectorThreadIsInterrupted() throws InterruptedException {
        var job = Thread.ofVirtual().start(() -> {
            var flow = Flows.<Integer>concurrentFlow(emitter -> {
                for (var i = 0; i < 100; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    emitter.emit(i);
                }
            });

            try {
                flow.collect(value -> {});
            } catch (Exception e) {
                assertInstanceOf(InterruptedException.class, e);
            }
        });

        Thread.sleep(500);
        job.interrupt();
        job.join(Duration.ofSeconds(2));
        assertFalse(job.isAlive(), "Concurrent flow should have been cancelled by emitter.emit()");
    }

    @Test
    void shouldAcceptNullValuesInEmissions() {
        var flow = Flows.<String>concurrentFlow(emitter -> {
            emitter.emit("first");
            emitter.emit(null);
            emitter.emit("third");
        });

        var list = new ArrayList<String>();
        flow.collect(list::add);
        assertEquals(3, list.size());
        assertTrue(list.contains(null));
        assertTrue(list.contains("first"));
        assertTrue(list.contains("third"));
    }

    @Test
    void shouldCompleteSuccessfullyWhenNoValuesAreEmitted() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            // No emissions
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);
        assertTrue(list.isEmpty());
    }

    @Test
    void shouldPropagateCheckedExceptionFromEmitterAfterEmissions() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            emitter.emit(1);
            throw new IOException("checked exception");
        });

        var exception = assertThrows(IOException.class, () -> flow.collect(v -> {}));
        assertEquals("checked exception", exception.getMessage());
    }

    @Test
    void shouldPropagateRuntimeExceptionFromEmitter() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            emitter.emit(1);
            throw new IllegalArgumentException("runtime exception");
        });

        var exception = assertThrows(IllegalArgumentException.class, () -> flow.collect(v -> {}));
        assertEquals("runtime exception", exception.getMessage());
    }

    @Test
    void shouldPropagateExceptionFromCollector() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
        });

        var exception = assertThrows(IOException.class, () ->
                flow.collect(value -> {
                    if (value == 2) {
                        throw new IOException("collector error");
                    }
                })
        );

        assertEquals("collector error", exception.getMessage());
    }

    @Test
    @SuppressWarnings("CatchMayIgnoreException")
    void shouldPropagateCollectorExceptionOnlyToEmitterThreadThatTriggeredIt() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.submit(() -> {
                    for (var i = 0; i < 10; i++) {
                        try {
                            Thread.sleep(100);
                            emitter.emit(i);
                        } catch (InterruptedException e) {
                            break;
                        } catch (Exception e) {
                            assertInstanceOf(IOException.class, e);
                            assertEquals("collector error", e.getMessage());
                            break;
                        }
                    }
                });

                executor.submit(() -> {
                    for (var i = 10; i < 20; i++) {
                        try {
                            Thread.sleep(100);
                            emitter.emit(i);
                        } catch (InterruptedException e) {
                            break;
                        } catch (Exception e) {
                            fail("should never be called");
                        }
                    }
                });
            }
        });

        var list = new LinkedList<Integer>();

        flow.collect(value -> {
            if (value == 5) {
                throw new IOException("collector error");
            }

            list.add(value);
        });

        assertEquals(15, list.size());
    }

    @Test
    void shouldSupportMultipleCollectionsOnSameFlow() {
        var counter = new AtomicInteger(0);
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            counter.incrementAndGet();
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        });

        var list1 = new ArrayList<Integer>();
        flow.collect(list1::add);
        assertEquals(3, list1.size());
        assertTrue(list1.containsAll(List.of(1, 2, 3)));
        assertEquals(1, counter.get());

        var list2 = new ArrayList<Integer>();
        flow.collect(list2::add);
        assertEquals(3, list2.size());
        assertTrue(list2.containsAll(List.of(1, 2, 3)));
        assertEquals(2, counter.get());
    }

    @Test
    void shouldHandleHighVolumeEmissions() {
        var count = 10_000;
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var i = 0; i < count; i++) {
                    final var value = i;
                    executor.submit(() -> emitter.emit(value));
                }
            }
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);
        assertEquals(count, list.size());

        var set = new HashSet<>(list);
        assertEquals(count, set.size());
    }

    @Test
    void shouldEnsureSerializedCollectorAccess() {
        var activeCollectors = new AtomicInteger(0);
        var maxConcurrentCollectors = new AtomicInteger(0);
        var violations = new AtomicInteger(0);

        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var i = 0; i < 1000; i++) {
                    final var value = i;
                    executor.submit(() -> emitter.emit(value));
                }
            }
        });

        flow.collect(value -> {
            var current = activeCollectors.incrementAndGet();
            maxConcurrentCollectors.updateAndGet(max -> Math.max(max, current));

            if (current > 1) {
                violations.incrementAndGet();
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            activeCollectors.decrementAndGet();
        });

        assertEquals(0, violations.get(), "Collector was called concurrently!");
        assertEquals(1, maxConcurrentCollectors.get(), "Collector should only be accessed by one thread at a time");
    }

    @Test
    void shouldHandleHighContentionScenario() {
        var threadCount = 100;
        var emissionsPerThread = 100;

        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            try (var executor = Executors.newFixedThreadPool(threadCount)) {
                var latch = new CountDownLatch(threadCount);

                for (var t = 0; t < threadCount; t++) {
                    final var threadId = t;
                    executor.submit(() -> {
                        try {
                            latch.countDown();
                            latch.await();

                            for (var i = 0; i < emissionsPerThread; i++) {
                                emitter.emit(threadId * emissionsPerThread + i);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);
        assertEquals(threadCount * emissionsPerThread, list.size());
    }

    @Test
    void shouldCollectSingleEmittedValue() {
        var flow = Flows.<String>concurrentFlow(emitter -> emitter.emit("only"));

        var list = new ArrayList<String>();
        flow.collect(list::add);
        assertEquals(List.of("only"), list);
    }

    @Test
    void shouldCollectEmissionsFromVirtualAndPlatformThreads() {
        var flow = Flows.<String>concurrentFlow(emitter -> {
            try (var virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var i = 0; i < 100; i++) {
                    final var value = "virtual-" + i;
                    virtualExecutor.submit(() -> emitter.emit(value));
                }
            }

            try (var platformExecutor = Executors.newFixedThreadPool(4)) {
                var tasks = new ArrayList<Future<?>>();
                for (var i = 0; i < 100; i++) {
                    final var value = "platform-" + i;
                    tasks.add(platformExecutor.submit(() -> emitter.emit(value)));
                }
                for (var task : tasks) {
                    try {
                        task.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            emitter.emit("main-thread");
        });

        var list = new ArrayList<String>();
        flow.collect(list::add);
        assertEquals(201, list.size());
    }
}
