package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DelayEachTest {
    @Test
    void shouldDelayEachWithoutChangingValues() {
        var values = Flows.of(1, 2, 3)
                .delayEach(Duration.ZERO)
                .toList();

        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void shouldDelayEachElementByPositiveDuration() {
        var startNanos = System.nanoTime();

        var values = Flows.of(1, 2)
                .delayEach(Duration.ofMillis(15))
                .toList();
        var elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertEquals(List.of(1, 2), values);
        assertTrue(elapsedMillis >= 20);
    }

    @Test
    void shouldRejectNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).delayEach(Duration.ofMillis(-1)));
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).delayEach(Duration.ofMillis(5));

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldCancelUpstreamWhenStoppedEarlyDuringElementDelay() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
            } finally {
                closed.countDown();
            }
        }).delayEach(Duration.ofMillis(10));

        assertEquals(List.of(1), flow.take(1).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
