package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LastTest {
    @Test
    void shouldReturnLastItem() {
        var flow = Flows.of(1, 2, 3);

        assertEquals(3, flow.last());
    }

    @Test
    void shouldReturnSingleItemWhenLengthIsOne() {
        assertEquals(42, Flows.of(42).last());
    }

    @Test
    void shouldThrowOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> Flows.<Integer>empty().last());
    }

    @Test
    void shouldPropagateUpstreamExceptionEvenAfterElementsEmitted() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            throw new IOException("boom");
        });

        assertThrows(IOException.class, flow::last);
    }
}
