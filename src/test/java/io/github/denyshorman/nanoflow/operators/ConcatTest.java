package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcatTest {
    @Test
    void shouldAppendOtherFlow() {
        var flow = Flows.of(1, 2).concat(Flows.of(3, 4));

        assertEquals(List.of(1, 2, 3, 4), flow.toList());
    }
}
