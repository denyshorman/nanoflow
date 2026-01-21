package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimerTest {
    @Test
    void shouldEmitSingleTick() {
        var values = Flows.timer(Duration.ZERO).toList();

        assertEquals(List.of(0L), values);
    }

    @Test
    void shouldRejectNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                Flows.timer(Duration.ofMillis(-1))
        );
    }
}
