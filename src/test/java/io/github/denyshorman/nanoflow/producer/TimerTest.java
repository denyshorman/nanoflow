package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimerTest {
    @Test
    void shouldEmitSingleTick() {
        var values = Flows.timer(Duration.ZERO).toList();

        assertEquals(List.of(0L), values);
    }

    @Test
    void shouldHonorPositiveDelayBeforeEmittingTick() {
        var startNanos = System.nanoTime();
        var values = Flows.timer(Duration.ofMillis(20)).toList();
        var elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertEquals(List.of(0L), values);
        assertTrue(elapsedMillis >= 15);
    }

    @Test
    void shouldRejectNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                Flows.timer(Duration.ofMillis(-1))
        );
    }
}
