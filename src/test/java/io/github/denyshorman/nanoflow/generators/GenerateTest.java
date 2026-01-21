package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void shouldReturnEmptyWhenCountIsZero() {
        var values = Flows.generate(0, () -> 1).toList();

        assertEquals(List.of(), values);
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Flows.generate(-1, () -> 1));
    }
}
