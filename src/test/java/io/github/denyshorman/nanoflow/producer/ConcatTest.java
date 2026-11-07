package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ConcatTest {
    @Test
    void shouldConcatFlows() {
        var flow = Flows.concat(
                Flows.of(1, 2),
                Flows.empty(),
                Flows.of(3)
        );

        assertEquals(List.of(1, 2, 3), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenNoFlowsProvided() {
        assertEquals(List.of(), Flows.<Integer>concat().toList());
    }

    @Test
    void shouldHandleSingleFlow() {
        assertEquals(List.of(1, 2), Flows.concat(Flows.of(1, 2)).toList());
    }

    @Test
    void shouldNotOpenSubsequentFlowsWhenEarlierFlowFails() {
        var thirdOpened = new AtomicBoolean(false);

        var flow = Flows.concat(
                Flows.of(1),
                Flows.error(new IOException("boom")),
                Flows.defer(() -> {
                    thirdOpened.set(true);
                    return Flows.of(2);
                })
        );

        assertThrows(IOException.class, flow::toList);
        assertFalse(thirdOpened.get());
    }

    @Test
    void shouldCancelCurrentAndSkipRemainingFlowsWhenConsumerStopsEarly() throws InterruptedException {
        var firstClosed = new CountDownLatch(1);
        var secondOpened = new AtomicBoolean(false);

        var first = Flows.<Integer>flow(emitter -> {
            try {
                var i = 1;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++);
                }
            } finally {
                firstClosed.countDown();
            }
        });

        var second = Flows.defer(() -> {
            secondOpened.set(true);
            return Flows.of(100);
        });

        assertEquals(List.of(1, 2), Flows.concat(first, second).take(2).toList());
        assertTrue(firstClosed.await(5, TimeUnit.SECONDS));
        assertFalse(secondOpened.get());
    }
}
