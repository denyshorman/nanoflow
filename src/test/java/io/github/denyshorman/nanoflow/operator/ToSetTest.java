package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToSetTest {
    @Test
    void shouldCollectToSet() {
        var expected = new LinkedHashSet<>(List.of(1, 2));

        assertEquals(expected, Flows.of(1, 1, 2).toSet());
    }
}
