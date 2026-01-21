package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToListTest {
    @Test
    void shouldCollectToList() {
        assertEquals(List.of(1, 2, 3), Flows.of(1, 2, 3).toList());
    }
}
