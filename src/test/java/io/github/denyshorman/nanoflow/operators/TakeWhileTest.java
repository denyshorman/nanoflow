package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TakeWhileTest {
    @Test
    void shouldTakeWhilePredicateMatches() {
        var flow = Flows.of(1, 2, 3, 2).takeWhile(value -> value < 3);

        assertEquals(List.of(1, 2), flow.toList());
    }
}
