package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcatTest {
    @Test
    void shouldConcatFlows() {
        var flow = Flows.concat(
                Flows.of(1, 2),
                Flows.<Integer>empty(),
                Flows.of(3)
        );

        assertEquals(List.of(1, 2, 3), flow.toList());
    }
}
