package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DebounceTest {
    @Test
    void shouldDebounceByTimeout() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            Thread.sleep(50);
            emitter.emit(2);
            Thread.sleep(200);
            emitter.emit(3);
        });

        var values = flow.debounce(Duration.ofMillis(100)).toList();

        assertEquals(List.of(2, 3), values);
    }

    @Test
    void shouldEmitLastValueOnCompletion() {
        var values = Flows.of(1, 2, 3)
                .debounce(Duration.ofSeconds(1))
                .toList();

        assertEquals(List.of(3), values);
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        assertEquals(List.of(), Flows.<Integer>empty().debounce(Duration.ofMillis(50)).toList());
    }

    @Test
    void shouldEmitPendingValueBeforePropagatingUpstreamException() {
        var received = new ArrayList<Integer>();

        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            throw new IOException("boom");
        }).debounce(Duration.ofSeconds(10));

        assertThrows(IOException.class, () -> flow.collect(received::add));
        assertEquals(List.of(2), received);
    }

    @Test
    void shouldCancelUpstreamPumpWhenConsumerStopsEarly() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        }).debounce(Duration.ofMillis(20));

        assertEquals(List.of(1), flow.take(1).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).debounce(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).debounce(Duration.ofMillis(-1)));
    }
}
