package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class IterateTest {
    @Test
    void shouldIterateValues() {
        var values = Flows.iterate(1, value -> value + 1).take(4).toList();

        assertEquals(List.of(1, 2, 3, 4), values);
    }

    @Test
    void shouldIterateWhilePredicateHolds() {
        var values = Flows.iterate(1, value -> value <= 3, value -> value + 1).toList();

        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void shouldReturnEmptyAndNotInvokeNextWhenSeedFailsPredicate() {
        var nextCalled = new AtomicBoolean(false);

        var values = Flows.iterate(10, value -> value < 5, value -> {
            nextCalled.set(true);
            return value + 1;
        }).toList();

        assertEquals(List.of(), values);
        assertFalse(nextCalled.get());
    }

    @Test
    void shouldPropagateNextFunctionException() {
        var flow = Flows.iterate(1, value -> {
            throw new IllegalStateException("next boom");
        });

        assertThrows(IllegalStateException.class, () -> flow.take(2).toList());
    }

    @Test
    void shouldPropagateHasNextPredicateException() {
        var flow = Flows.iterate(1, value -> {
            throw new IllegalStateException("hasNext boom");
        }, value -> value + 1);

        assertThrows(IllegalStateException.class, flow::toList);
    }
}
