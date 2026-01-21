package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountTest {
    @Test
    void shouldCountItems() {
        assertEquals(3L, Flows.of(1, 2, 3).count());
    }

    @Test
    void shouldReturnZeroForEmpty() {
        assertEquals(0L, Flows.<Integer>empty().count());
    }
}
