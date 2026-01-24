package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatMapTest {
    @Test
    void shouldFlattenInnerFlows() {
        var flow = Flows.of(1, 2).flatMap(value -> Flows.of(value, value * 10));

        assertEquals(List.of(1, 10, 2, 20), flow.toList());
    }
}
