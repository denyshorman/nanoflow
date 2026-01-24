package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoneTest {
    @Test
    void shouldReturnTrueWhenNoneMatch() {
        assertTrue(Flows.of(1, 2, 3).none(value -> value < 0));
    }

    @Test
    void shouldReturnFalseWhenAnyMatches() {
        assertFalse(Flows.of(1, 2, 3).none(value -> value == 2));
    }
}
