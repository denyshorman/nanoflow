package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamTest {
    @Test
    void shouldExposeStreamView() {
        try (var stream = Flows.of(1, 2, 3).stream()) {
            assertEquals(List.of(1, 2, 3), stream.toList());
        }
    }
}
