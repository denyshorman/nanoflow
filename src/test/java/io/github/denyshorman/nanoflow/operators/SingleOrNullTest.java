package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SingleOrNullTest {
    @Test
    void shouldReturnNullOnEmpty() {
        assertNull(Flows.<Integer>empty().singleOrNull());
    }

    @Test
    void shouldThrowOnMultipleItems() {
        assertThrows(IllegalStateException.class, () -> Flows.of(1, 2).singleOrNull());
    }
}
