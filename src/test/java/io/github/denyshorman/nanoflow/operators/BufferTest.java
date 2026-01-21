package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BufferTest {
    @Test
    void shouldPreserveValues() {
        var flow = Flows.of(1, 2, 3).buffer(1);

        assertEquals(List.of(1, 2, 3), flow.toList());
    }
}
