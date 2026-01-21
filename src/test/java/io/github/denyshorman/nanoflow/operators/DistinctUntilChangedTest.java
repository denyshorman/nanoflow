package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistinctUntilChangedTest {
    @Test
    void shouldSkipAdjacentDuplicates() {
        var flow = Flows.of(1, 1, 2, 2, 1).distinctUntilChanged();

        assertEquals(List.of(1, 2, 1), flow.toList());
    }
}
