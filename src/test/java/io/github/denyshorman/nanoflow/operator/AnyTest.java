package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnyTest {
    @Test
    void shouldReturnTrueWhenPredicateMatches() {
        assertTrue(Flows.of(1, 2, 3).any(value -> value == 2));
    }

    @Test
    void shouldReturnFalseWhenPredicateDoesNotMatch() {
        assertFalse(Flows.of(1, 2, 3).any(value -> value == 5));
    }
}
