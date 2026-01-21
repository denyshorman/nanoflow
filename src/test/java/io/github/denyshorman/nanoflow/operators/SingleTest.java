package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SingleTest {
    @Test
    void shouldReturnSingleItem() {
        assertEquals(1, Flows.of(1).single());
    }

    @Test
    void shouldThrowOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> Flows.<Integer>empty().single());
    }

    @Test
    void shouldThrowOnMultipleItems() {
        assertThrows(IllegalStateException.class, () -> Flows.of(1, 2).single());
    }
}
