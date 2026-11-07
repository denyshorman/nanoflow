package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {
    @Test
    void shouldFilterValues() {
        var flow = Flows.of(1, 2, 3, 4).filter(value -> value % 2 == 0);

        assertEquals(List.of(2, 4), flow.toList());
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        var flow = Flows.<Integer>empty().filter(value -> true);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenNoneMatch() {
        var flow = Flows.of(1, 3, 5).filter(value -> value % 2 == 0);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldReturnAllWhenAllMatch() {
        var flow = Flows.of(2, 4, 6).filter(value -> value % 2 == 0);

        assertEquals(List.of(2, 4, 6), flow.toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).filter(value -> true);

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldPropagatePredicateExceptionAndCancelUpstream() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
            } finally {
                closed.countDown();
            }
        }).filter(value -> {
            if (value == 2) {
                throw new IllegalStateException("predicate boom");
            }
            return true;
        });

        assertThrows(IllegalStateException.class, flow::toList);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldCancelInfiniteUpstreamWhenDownstreamStopsEarly() throws InterruptedException {
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
        }).filter(value -> value % 3 == 0);

        assertEquals(List.of(3, 6), flow.take(2).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
