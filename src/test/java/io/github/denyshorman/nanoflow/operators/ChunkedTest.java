package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkedTest {
    @Test
    void shouldChunkItems() {
        var flow = Flows.of(1, 2, 3, 4, 5).chunked(2);

        assertEquals(List.of(
                List.of(1, 2),
                List.of(3, 4),
                List.of(5)
        ), flow.toList());
    }

    @Test
    void shouldRejectNonPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(0));
    }
}
