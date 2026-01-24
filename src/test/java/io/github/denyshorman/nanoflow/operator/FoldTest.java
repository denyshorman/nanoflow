package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoldTest {
    @Test
    void shouldFoldValues() {
        var result = Flows.of(1, 2, 3).fold(10, Integer::sum);

        assertEquals(16, result);
    }
}
