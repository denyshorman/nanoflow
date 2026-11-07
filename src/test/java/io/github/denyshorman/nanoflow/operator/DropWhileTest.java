package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DropWhileTest {
    @Test
    void shouldDropWhilePredicateMatches() {
        var flow = Flows.of(1, 2, 3, 1).dropWhile(value -> value < 3);

        assertEquals(List.of(3, 1), flow.toList());
    }

    @Test
    void shouldReturnAllWhenFirstDoesNotMatch() {
        var flow = Flows.of(3, 1, 2).dropWhile(value -> value < 3);

        assertEquals(List.of(3, 1, 2), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenAllMatch() {
        var flow = Flows.of(1, 2).dropWhile(value -> value < 3);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).dropWhile(value -> true);

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
        }).dropWhile(value -> {
            if (value == 2) {
                throw new IllegalStateException("predicate boom");
            }
            return true;
        });

        assertThrows(IllegalStateException.class, flow::toList);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldNotInvokePredicateAfterFirstNonMatchingElement() {
        var calls = new AtomicInteger();

        var flow = Flows.of(1, 2, 3, 4, 5).dropWhile(value -> {
            calls.incrementAndGet();
            return value < 3;
        });

        assertEquals(List.of(3, 4, 5), flow.toList());
        assertEquals(3, calls.get());
    }
}
