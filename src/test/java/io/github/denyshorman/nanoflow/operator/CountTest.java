package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CountTest {
    @Test
    void shouldCountItems() {
        assertEquals(3L, Flows.of(1, 2, 3).count());
    }

    @Test
    void shouldReturnZeroForEmpty() {
        assertEquals(0L, Flows.<Integer>empty().count());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IOException("boom");
        });

        assertThrows(IOException.class, flow::count);
    }
}
