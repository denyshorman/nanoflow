package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReduceTest {
    @Test
    void shouldReduceValues() {
        assertEquals(6, Flows.of(1, 2, 3).reduce(Integer::sum));
    }

    @Test
    void shouldThrowOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> Flows.<Integer>empty().reduce(Integer::sum));
    }
}
