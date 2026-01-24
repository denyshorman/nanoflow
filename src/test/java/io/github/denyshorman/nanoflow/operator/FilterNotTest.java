package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilterNotTest {
    @Test
    void shouldExcludeMatchingValues() {
        var flow = Flows.of(1, 2, 3, 4).filterNot(value -> value % 2 == 0);

        assertEquals(List.of(1, 3), flow.toList());
    }
}
