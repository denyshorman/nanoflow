package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FlowTest {
    @Test
    void shouldCollectAllValuesInOrder() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        });

        var list = new LinkedList<Integer>();
        flow.collect(list::add);
        assertEquals(3, list.size());
        assertEquals(1, list.getFirst());
        assertEquals(2, list.get(1));
        assertEquals(3, list.getLast());
    }

    @Test
    void shouldAcceptNullValuesInEmissions() {
        var flow = Flows.<String>flow(emitter -> {
            emitter.emit("first");
            emitter.emit(null);
            emitter.emit("third");
        });

        var list = new ArrayList<String>();
        flow.collect(list::add);
        assertEquals(3, list.size());
        assertEquals("first", list.get(0));
        assertNull(list.get(1));
        assertEquals("third", list.get(2));
    }

    @Test
    void shouldCompleteSuccessfullyWhenNoValuesAreEmitted() {
        var flow = Flows.<Integer>flow(emitter -> {
            // No emissions
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);
        assertTrue(list.isEmpty());
    }

    @Test
    void shouldSupportBlockingOperationsInEmitter() {
        var flow = Flows.<Integer>flow(emitter -> {
            Thread.sleep(10);
            emitter.emit(1);
        });

        var list = new LinkedList<Integer>();
        flow.collect(list::add);
        assertEquals(1, list.size());
    }

    @Test
    void shouldStopEmittingWhenCollectorThreadIsInterrupted() throws InterruptedException {
        var job = Thread.ofVirtual().start(() -> {
            var flow = Flows.<Integer>flow(emitter -> {
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
                fail("should not throw from simple flows");
            }
        });

        Thread.sleep(500);
        job.interrupt();
        job.join();
        assertFalse(job.isAlive(), "Simple flow should have been cancelled by emitter.emit()");
    }

    @Test
    void shouldPropagateCheckedExceptionFromEmitter() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IOException("checked exception");
        });

        var exception = assertThrows(IOException.class, () -> flow.collect(v -> {}));
        assertEquals("checked exception", exception.getMessage());
    }

    @Test
    void shouldPropagateRuntimeExceptionFromEmitter() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IllegalStateException("runtime exception");
        });

        var exception = assertThrows(IllegalStateException.class, () -> flow.collect(v -> {}));
        assertEquals("runtime exception", exception.getMessage());
    }

    @Test
    void shouldPropagateExceptionFromCollector() {
        var flow = Flows.<Integer>flow(emitter -> {
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
    void shouldSupportMultipleCollectionsOnSameFlow() {
        var counter = new AtomicInteger(0);

        var flow = Flows.<Integer>flow(emitter -> {
            counter.incrementAndGet();
            emitter.emit(1);
            emitter.emit(2);
        });

        var list1 = new ArrayList<Integer>();
        flow.collect(list1::add);
        assertEquals(List.of(1, 2), list1);
        assertEquals(1, counter.get());

        var list2 = new ArrayList<Integer>();
        flow.collect(list2::add);
        assertEquals(List.of(1, 2), list2);
        assertEquals(2, counter.get());

        var list3 = new ArrayList<Integer>();
        flow.collect(list3::add);
        assertEquals(List.of(1, 2), list3);
        assertEquals(3, counter.get());
    }

    @Test
    void shouldMaintainOrderOfEmissions() {
        var flow = Flows.<Integer>flow(emitter -> {
            for (var i = 0; i < 10; i++) {
                emitter.emit(i);
            }
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);

        for (var i = 0; i < 10; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    void shouldCollectSingleEmittedValue() {
        var flow = Flows.<String>flow(emitter -> emitter.emit("only"));
        var list = new ArrayList<String>();
        flow.collect(list::add);
        assertEquals(List.of("only"), list);
    }

    @Test
    void shouldHandleLargeNumberOfEmissions() {
        var count = 100_000;

        var flow = Flows.<Integer>flow(emitter -> {
            for (var i = 0; i < count; i++) {
                emitter.emit(i);
            }
        });

        var counter = new AtomicInteger(0);
        flow.collect(value -> counter.incrementAndGet());
        assertEquals(count, counter.get());
    }

    @Test
    void shouldSupportCustomRecordTypes() {
        record Person(String name, int age) {
        }

        var flow = Flows.<Person>flow(emitter -> {
            emitter.emit(new Person("Alice", 30));
            emitter.emit(new Person("Bob", 25));
            emitter.emit(null);
            emitter.emit(new Person("Charlie", 35));
        });

        var list = new ArrayList<Person>();
        flow.collect(list::add);
        assertEquals(4, list.size());
        assertEquals("Alice", list.get(0).name());
        assertEquals("Bob", list.get(1).name());
        assertNull(list.get(2));
        assertEquals("Charlie", list.get(3).name());
    }

    @Test
    void shouldHaveMinimalOverheadForHighVolumeEmissions() {
        var iterations = 100_000;
        var counter = new AtomicInteger(0);
        var start = System.nanoTime();

        var flow = Flows.<Integer>flow(emitter -> {
            for (var i = 0; i < iterations; i++) {
                emitter.emit(i);
            }
        });

        flow.collect(value -> counter.incrementAndGet());

        var duration = System.nanoTime() - start;

        assertEquals(iterations, counter.get());
        assertTrue(duration < 5_000_000_000L, "Simple flow took too long: " + duration + "ns");
    }
}
