package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutTest {
    @Test
    void shouldPassWhenValuesArrive() {
        var values = Flows.of(1, 2)
                .timeout(Duration.ofSeconds(30))
                .toList();

        assertEquals(List.of(1, 2), values);
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        assertEquals(List.of(), Flows.<Integer>empty().timeout(Duration.ofMillis(10)).toList());
    }

    @Test
    void shouldTimeoutWhenNoValueArrives() {
        assertThrows(TimeoutException.class, () -> Flows.<Integer>never().timeout(Duration.ofMillis(10)).toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).timeout(Duration.ofSeconds(1));

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldTimeoutBetweenElementsAndCancelUpstream() throws InterruptedException {
        var closed = new CountDownLatch(1);
        var received = new ArrayList<Integer>();

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        }).timeout(Duration.ofMillis(20));

        assertThrows(TimeoutException.class, () -> flow.collect(received::add));
        assertEquals(List.of(1), received);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(2)
    void shouldNotCountDownstreamProcessingTimeAgainstTimeout() throws InterruptedException {
        var received = new ArrayList<Integer>();

        var flow = Flows.of(1, 2, 3).timeout(Duration.ofMillis(20));

        flow.collect(value -> {
            Thread.sleep(50);
            received.add(value);
        });

        assertEquals(List.of(1, 2, 3), received);
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).timeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).timeout(Duration.ofMillis(-1)));
    }
}
