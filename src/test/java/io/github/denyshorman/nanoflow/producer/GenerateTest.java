package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTest {
    @Test
    void shouldGenerateValues() {
        var counter = new AtomicInteger();
        var values = Flows.generate(counter::getAndIncrement).take(3).toList();

        assertEquals(List.of(0, 1, 2), values);
    }

    @Test
    void shouldGenerateFixedCount() {
        var counter = new AtomicInteger();
        var values = Flows.generate(3, counter::getAndIncrement).toList();

        assertEquals(List.of(0, 1, 2), values);
    }

    @Test
    void shouldReturnEmptyAndNotInvokeSupplierWhenCountIsZero() {
        var invoked = new AtomicBoolean(false);
        var values = Flows.generate(0, () -> {
            invoked.set(true);
            return 1;
        }).toList();

        assertEquals(List.of(), values);
        assertFalse(invoked.get());
    }

    @Test
    void shouldPropagateSupplierException() {
        var flow = Flows.generate(() -> {
            throw new IllegalStateException("supplier boom");
        });

        assertThrows(IllegalStateException.class, flow::first);
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Flows.generate(-1, () -> 1));
    }
}
