package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TakeWhileTest {
    @Test
    void shouldTakeWhilePredicateMatches() {
        var flow = Flows.of(1, 2, 3, 2).takeWhile(value -> value < 3);

        assertEquals(List.of(1, 2), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenFirstElementDoesNotMatch() {
        var flow = Flows.of(5, 1, 2).takeWhile(value -> value < 3);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldReturnAllWhenAllElementsMatch() {
        var flow = Flows.of(1, 2).takeWhile(value -> value < 3);

        assertEquals(List.of(1, 2), flow.toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).takeWhile(value -> true);

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
        }).takeWhile(value -> {
            if (value == 2) {
                throw new IllegalStateException("predicate boom");
            }
            return true;
        });

        assertThrows(IllegalStateException.class, flow::toList);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(2)
    void shouldCancelUpstreamImmediatelyWhenPredicateReturnsFalse() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        }).takeWhile(value -> value < 3);

        assertEquals(List.of(1, 2), flow.toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
