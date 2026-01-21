package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllTest {
    @Test
    void shouldReturnTrueWhenAllMatch() {
        assertTrue(Flows.of(1, 2, 3).all(value -> value > 0));
    }

    @Test
    void shouldReturnFalseWhenAnyDoesNotMatch() {
        assertFalse(Flows.of(1, 2, 3).all(value -> value < 3));
    }
}
