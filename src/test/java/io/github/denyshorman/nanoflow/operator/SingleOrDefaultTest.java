package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SingleOrDefaultTest {
    @Test
    void shouldReturnDefaultOnEmpty() {
        assertEquals(7, Flows.<Integer>empty().singleOrDefault(7));
    }

    @Test
    void shouldThrowOnMultipleItems() {
        assertThrows(IllegalStateException.class, () -> Flows.of(1, 2).singleOrDefault(1));
    }
}
