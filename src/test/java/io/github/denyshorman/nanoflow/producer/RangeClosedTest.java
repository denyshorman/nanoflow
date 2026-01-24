package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeClosedTest {
    @Test
    void shouldCreateClosedRange() {
        var values = Flows.rangeClosed(1, 3).toList();

        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void shouldReturnEmptyWhenStartAfterEnd() {
        assertEquals(List.of(), Flows.rangeClosed(3, 2).toList());
    }
}
