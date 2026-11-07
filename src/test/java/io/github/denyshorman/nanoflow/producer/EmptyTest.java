package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class EmptyTest {
    @Test
    void shouldReturnEmptyFlow() {
        var values = Flows.<Integer>empty().toList();

        assertEquals(List.of(), values);
    }

    @Test
    void shouldReuseSingletonInstance() {
        var first = Flows.<Integer>empty();
        var second = Flows.<Integer>empty();

        assertSame(first, second);
        assertSame(first.open(), second.open());
    }

    @Test
    void shouldEnforceEmptyIteratorContract() {
        try (var sequence = Flows.<Integer>empty().open()) {
            var iterator = sequence.iterator();
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }
}
