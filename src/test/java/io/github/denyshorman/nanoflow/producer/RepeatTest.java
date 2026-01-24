package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepeatTest {
    @Test
    void shouldRepeatValue() {
        var values = Flows.repeat("a").take(3).toList();

        assertEquals(List.of("a", "a", "a"), values);
    }

    @Test
    void shouldRepeatFixedCount() {
        var values = Flows.repeat(2, "x").toList();

        assertEquals(List.of("x", "x"), values);
    }

    @Test
    void shouldReturnEmptyWhenCountIsZero() {
        var values = Flows.repeat(0, "x").toList();

        assertEquals(List.of(), values);
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Flows.repeat(-1, "x"));
    }
}
