package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapTest {
    @Test
    void shouldMapValues() {
        var flow = Flows.of(1, 2, 3).map(value -> value * 2);

        assertEquals(List.of(2, 4, 6), flow.toList());
    }
}
