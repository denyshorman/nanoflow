package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToSetTest {
    @Test
    void shouldCollectToSet() {
        var expected = new LinkedHashSet<>(List.of(1, 2));

        assertEquals(expected, Flows.of(1, 1, 2).toSet());
    }

    @Test
    void shouldPreserveEncounterOrder() {
        var set = Flows.of(3, 1, 2, 3, 1).toSet();

        assertEquals(List.of(3, 1, 2), new ArrayList<>(set));
    }

    @Test
    void shouldReturnEmptySetForEmptyFlow() {
        assertEquals(Set.of(), Flows.<Integer>empty().toSet());
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).toSet());
    }
}
