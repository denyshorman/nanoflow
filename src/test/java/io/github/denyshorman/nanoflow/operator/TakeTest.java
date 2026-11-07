package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

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
    void shouldReturnAllWhenCountExceedsLength() {
        var flow = Flows.of(1, 2).take(10);

        assertEquals(List.of(1, 2), flow.toList());
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).take(-1));
    }

    @Test
    void shouldNotOpenUpstreamWhenTakingZero() {
        var opened = new AtomicBoolean(false);
        var flow = Flows.defer(() -> {
            opened.set(true);
            return Flows.of(1, 2, 3);
        }).take(0);

        assertEquals(List.of(), flow.toList());
        assertFalse(opened.get());
    }

    @Test
    void shouldPropagateUpstreamExceptionBeforeLimitReached() {
        var flow = Flows.<Integer>error(new IOException("boom")).take(2);

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    @Timeout(2)
    void shouldCompleteImmediatelyOnNthElementWithoutWaitingForNextItemOrError() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        }).take(2);

        assertEquals(List.of(1, 2), flow.toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
