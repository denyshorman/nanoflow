package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistinctTest {
    @Test
    void shouldKeepFirstOccurrences() {
        var flow = Flows.of(1, 2, 2, 3, 1).distinct();

        assertEquals(List.of(1, 2, 3), flow.toList());
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        assertEquals(List.of(), Flows.<Integer>empty().distinct().toList());
    }

    @Test
    void shouldMaintainSeparateSeenSetPerSubscription() {
        var flow = Flows.of(1, 2, 1, 3).distinct();

        assertEquals(List.of(1, 2, 3), flow.toList());
        assertEquals(List.of(1, 2, 3), flow.toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).distinct();

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldCancelInfiniteUpstreamWhenDownstreamStopsEarly() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                var i = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++ / 2);
                }
            } finally {
                closed.countDown();
            }
        }).distinct();

        assertEquals(List.of(0, 1, 2), flow.take(3).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
