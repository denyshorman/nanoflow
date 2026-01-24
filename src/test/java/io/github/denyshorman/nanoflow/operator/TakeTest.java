package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TakeTest {
    @Test
    void shouldTakeLimitedAmount() {
        var flow = Flows.of(1, 2, 3).take(2);

        assertEquals(List.of(1, 2), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenTakingZero() {
        var flow = Flows.of(1, 2, 3).take(0);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).take(-1));
    }
}
