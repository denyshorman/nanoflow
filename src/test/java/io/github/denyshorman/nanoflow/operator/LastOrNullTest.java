package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LastOrNullTest {
    @Test
    void shouldReturnLastItem() {
        var flow = Flows.of(1, 2, 3);

        assertEquals(3, flow.lastOrNull());
    }

    @Test
    void shouldReturnNullOnEmpty() {
        assertNull(Flows.<Integer>empty().lastOrNull());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IOException("boom");
        });

        assertThrows(IOException.class, flow::lastOrNull);
    }
}
