package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ScanTest {
    @Test
    void shouldEmitRunningAccumulations() {
        var flow = Flows.of(1, 2, 3).scan(0, Integer::sum);

        assertEquals(List.of(0, 1, 3, 6), flow.toList());
    }

    @Test
    void shouldEmitInitialValueForEmptyFlow() {
        var flow = Flows.<Integer>empty().scan(10, Integer::sum);

        assertEquals(List.of(10), flow.toList());
    }

    @Test
    void shouldAccumulateIntoDifferentType() {
        var flow = Flows.of("a", "bb", "ccc")
                .scan(0, (len, str) -> len + str.length());

        assertEquals(List.of(0, 1, 3, 6), flow.toList());
    }

    @Test
    void shouldEmitInitialValueBeforePropagatingUpstreamException() {
        var received = new ArrayList<Integer>();
        var flow = Flows.<Integer>error(new IOException("boom"))
                .scan(42, Integer::sum);

        assertThrows(IOException.class, () -> flow.collect(received::add));
        assertEquals(List.of(42), received);
    }

    @Test
    void shouldPropagateReducerExceptionAndCancelUpstream() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
            } finally {
                closed.countDown();
            }
        }).scan(0, (acc, item) -> {
            if (item == 2) {
                throw new IllegalStateException("reducer boom");
            }
            return acc + item;
        });

        var received = new ArrayList<Integer>();
        assertThrows(IllegalStateException.class, () -> flow.collect(received::add));
        assertEquals(List.of(0, 1), received);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldCancelInfiniteUpstreamWhenStoppedEarly() throws InterruptedException {
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
        }).scan(0, Integer::sum);

        assertEquals(List.of(0, 1, 3), flow.take(3).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
