package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanTest {
    @Test
    void shouldEmitRunningAccumulations() {
        var flow = Flows.of(1, 2, 3).scan(0, Integer::sum);

        assertEquals(List.of(0, 1, 3, 6), flow.toList());
    }

    @Test
    void shouldEmitInitialValueForEmptyFlow() {
        var flow = Flows.<Integer>empty().scan(10, Integer::sum);

        assertEquals(List.of(10), flow.toList());
    }
}
