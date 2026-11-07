package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DelayTest {
    @Test
    void shouldDelayStartWithoutChangingValues() {
        var values = Flows.of(1, 2, 3)
                .delay(Duration.ZERO)
                .toList();

        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void shouldDelayStartByPositiveDuration() {
        var startNanos = System.nanoTime();

        var values = Flows.of(1, 2)
                .delay(Duration.ofMillis(20))
                .toList();
        var elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertEquals(List.of(1, 2), values);
        assertTrue(elapsedMillis >= 15);
    }

    @Test
    void shouldRejectNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).delay(Duration.ofMillis(-1)));
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).delay(Duration.ofMillis(5));

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldNotOpenUpstreamIfCancelledDuringInitialDelay() throws InterruptedException {
        var opened = new AtomicBoolean(false);
        var started = new CountDownLatch(1);

        var flow = Flows.defer(() -> {
            opened.set(true);
            return Flows.of(1, 2, 3);
        }).delay(Duration.ofSeconds(10));

        var consumer = Thread.ofVirtual().start(() -> {
            try (var items = flow.open()) {
                started.countDown();
                for (var ignored : items) {
                    break;
                }
            } catch (Throwable ignored) {
            }
        });

        assertTrue(started.await(5, TimeUnit.SECONDS));
        Thread.sleep(15);
        consumer.interrupt();
        consumer.join(5000);

        assertFalse(opened.get());
    }
}
