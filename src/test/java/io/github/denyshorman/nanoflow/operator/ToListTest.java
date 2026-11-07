package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToListTest {
    @Test
    void shouldCollectToList() {
        assertEquals(List.of(1, 2, 3), Flows.of(1, 2, 3).toList());
    }

    @Test
    void shouldReturnEmptyListForEmptyFlow() {
        assertEquals(List.of(), Flows.<Integer>empty().toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).toList());
    }
}
