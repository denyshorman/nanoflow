package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelayTest {
    @Test
    void shouldDelayStartWithoutChangingValues() {
        var values = Flows.of(1, 2, 3)
                .delay(Duration.ZERO)
                .toList();

        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void shouldRejectNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).delay(Duration.ofMillis(-1)));
    }
}
