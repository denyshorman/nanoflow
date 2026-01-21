package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilterTest {
    @Test
    void shouldFilterValues() {
        var flow = Flows.of(1, 2, 3, 4).filter(value -> value % 2 == 0);

        assertEquals(List.of(2, 4), flow.toList());
    }
}
