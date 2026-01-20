package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CollectTest {
    @Test
    void shouldCollectAllValuesFromSimpleFlow() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.emit(4);
            emitter.emit(5);
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);

        assertEquals(List.of(1, 2, 3, 4, 5), list);
    }

    @Test
    void shouldCollectAllValuesFromConcurrentFlow() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.emit(4);
            emitter.emit(5);
        });

        var list = new LinkedList<Integer>();
        flow.collect(list::add);

        assertEquals(5, list.size());
        assertTrue(list.containsAll(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    void shouldCollectAllValuesFromConcurrentEmissions() {
        var flow = Flows.<Integer>concurrentFlow(emitter -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var i = 0; i < 100; i++) {
                    final var value = i;
                    executor.submit(() -> emitter.emit(value));
                }
            }
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);

        assertEquals(100, list.size());

        for (var i = 0; i < 100; i++) {
            assertTrue(list.contains(i), "List should contain " + i);
        }
    }

    @Test
    void shouldCollectNullValues() {
        var flow = Flows.<String>flow(emitter -> {
            emitter.emit("first");
            emitter.emit(null);
            emitter.emit("third");
            emitter.emit(null);
        });

        var list = new ArrayList<String>();
        flow.collect(list::add);

        assertEquals(4, list.size());
        assertEquals("first", list.get(0));
        assertNull(list.get(1));
        assertEquals("third", list.get(2));
        assertNull(list.get(3));
    }

    @Test
    void shouldCompleteWhenEmptyFlow() {
        var flow = Flows.<Integer>emptyFlow();

        var list = new ArrayList<Integer>();
        flow.collect(list::add);

        assertTrue(list.isEmpty());
    }

    @Test
    void shouldCompleteWhenFlowEmitsNothing() {
        var flow = Flows.<Integer>flow(emitter -> {
            // No emissions
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);

        assertTrue(list.isEmpty());
    }

    @Test
    void shouldInvokeCollectorForEachEmittedValue() {
        var flow = Flows.<String>flow(emitter -> {
            emitter.emit("a");
            emitter.emit("b");
            emitter.emit("c");
        });

        var counter = new AtomicInteger(0);
        flow.collect(value -> counter.incrementAndGet());

        assertEquals(3, counter.get());
    }

    @Test
    void shouldPropagateCheckedExceptionFromFlowAction() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IOException("flow error");
        });

        var exception = assertThrows(IOException.class, () -> flow.collect(value -> {
        }));
        assertEquals("flow error", exception.getMessage());
    }

    @Test
    void shouldPropagateCheckedExceptionFromCollector() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
        });

        var exception = assertThrows(IOException.class, () -> flow.collect(value -> {
            if (value == 2) {
                throw new IOException("collector error");
            }
        }));

        assertEquals("collector error", exception.getMessage());
    }

    @Test
    void shouldPropagateRuntimeExceptionFromFlowAction() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IllegalStateException("runtime error");
        });

        var exception = assertThrows(IllegalStateException.class, () -> flow.collect(value -> {
        }));
        assertEquals("runtime error", exception.getMessage());
    }

    @Test
    void shouldPropagateRuntimeExceptionFromCollector() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        });

        var exception = assertThrows(IllegalArgumentException.class, () -> flow.collect(value -> {
            if (value == 2) {
                throw new IllegalArgumentException("invalid value");
            }
        }));

        assertEquals("invalid value", exception.getMessage());
    }

    @Test
    void shouldCollectLargeNumberOfValues() {
        var count = 10000;
        var flow = Flows.<Integer>flow(emitter -> {
            for (var i = 0; i < count; i++) {
                emitter.emit(i);
            }
        });

        var list = new ArrayList<Integer>();
        flow.collect(list::add);

        assertEquals(count, list.size());

        for (var i = 0; i < count; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    void shouldSupportMultipleCollectionsOnSameFlow() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        });

        var list1 = new ArrayList<Integer>();
        flow.collect(list1::add);

        var list2 = new ArrayList<Integer>();
        flow.collect(list2::add);

        assertEquals(list1, list2);
        assertEquals(List.of(1, 2, 3), list1);
    }

    @Test
    void shouldExecuteCollectorOnCallingThread() {
        var flow = Flows.<Integer>flow(emitter -> emitter.emit(1));
        var collectorThread = new AtomicInteger(0);
        var currentThread = Thread.currentThread().threadId();
        flow.collect(value -> collectorThread.set((int) Thread.currentThread().threadId()));
        assertEquals(currentThread, collectorThread.get());
    }

    @Test
    void shouldHandleCollectorThatDoesNothing() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        });

        assertDoesNotThrow(() -> flow.collect(value -> {
        }));
    }

    @Test
    void shouldHandleEmissionsWithBlockingOperations() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            Thread.sleep(10);
            emitter.emit(2);
            Thread.sleep(10);
            emitter.emit(3);
        });

        var list = new ArrayList<Integer>();
        assertDoesNotThrow(() -> flow.collect(list::add));
        assertEquals(List.of(1, 2, 3), list);
    }

    @Test
    void shouldHandleCollectorWithBlockingOperations() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        });

        var list = new ArrayList<Integer>();
        assertDoesNotThrow(() -> flow.collect(value -> {
            Thread.sleep(10);
            list.add(value);
        }));
        assertEquals(List.of(1, 2, 3), list);
    }

    @Test
    void shouldCollectMixedTypesCorrectly() {
        var flow = Flows.flow(emitter -> {
            emitter.emit("string");
            emitter.emit(42);
            emitter.emit(3.14);
            emitter.emit(true);
        });

        var list = new ArrayList<>();
        flow.collect(list::add);

        assertEquals(4, list.size());
        assertEquals("string", list.get(0));
        assertEquals(42, list.get(1));
        assertEquals(3.14, list.get(2));
        assertEquals(true, list.get(3));
    }
}
