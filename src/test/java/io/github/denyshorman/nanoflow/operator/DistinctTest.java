package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistinctTest {
    @Test
    void shouldKeepFirstOccurrences() {
        var flow = Flows.of(1, 2, 2, 3, 1).distinct();

        assertEquals(List.of(1, 2, 3), flow.toList());
    }
}
