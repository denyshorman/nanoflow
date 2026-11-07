package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LastOrDefaultTest {
    @Test
    void shouldReturnLastItemWhenPresent() {
        assertEquals(3, Flows.of(1, 2, 3).lastOrDefault(9));
    }

    @Test
    void shouldReturnDefaultOnEmpty() {
        assertEquals(9, Flows.<Integer>empty().lastOrDefault(9));
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IOException("boom");
        });

        assertThrows(IOException.class, () -> flow.lastOrDefault(9));
    }
}
