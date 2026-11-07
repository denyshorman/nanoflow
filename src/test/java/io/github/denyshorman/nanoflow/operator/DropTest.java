package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DropTest {
    @Test
    void shouldDropLeadingItems() {
        var flow = Flows.of(1, 2, 3).drop(2);

        assertEquals(List.of(3), flow.toList());
    }

    @Test
    void shouldReturnAllWhenDroppingZero() {
        var flow = Flows.of(1, 2, 3).drop(0);

        assertEquals(List.of(1, 2, 3), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenCountExceedsLength() {
        var flow = Flows.of(1, 2).drop(5);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).drop(-1));
    }

    @Test
    void shouldPropagateUpstreamExceptionWhileDropping() {
        var flow = Flows.<Integer>error(new IOException("boom")).drop(5);

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldMaintainIndependentCountersAcrossMultipleCollections() {
        var flow = Flows.of(1, 2, 3, 4).drop(2);

        assertEquals(List.of(3, 4), flow.toList());
        assertEquals(List.of(3, 4), flow.toList());
    }

    @Test
    void shouldCancelUpstreamWhenStoppedAfterDrop() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                var i = 1;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++);
                }
            } finally {
                closed.countDown();
            }
        }).drop(3);

        assertEquals(List.of(4, 5), flow.take(2).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
