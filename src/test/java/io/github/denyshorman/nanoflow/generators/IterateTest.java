package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
