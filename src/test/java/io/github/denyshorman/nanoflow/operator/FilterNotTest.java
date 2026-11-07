package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FilterNotTest {
    @Test
    void shouldExcludeMatchingValues() {
        var flow = Flows.of(1, 2, 3, 4).filterNot(value -> value % 2 == 0);

        assertEquals(List.of(1, 3), flow.toList());
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        var flow = Flows.<Integer>empty().filterNot(value -> false);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenAllMatchPredicate() {
        var flow = Flows.of(2, 4, 6).filterNot(value -> value % 2 == 0);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldReturnAllWhenNoneMatchPredicate() {
        var flow = Flows.of(1, 3, 5).filterNot(value -> value % 2 == 0);

        assertEquals(List.of(1, 3, 5), flow.toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).filterNot(value -> false);

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
        }).filterNot(value -> {
            if (value == 2) {
                throw new IllegalStateException("predicate boom");
            }
            return false;
        });

        assertThrows(IllegalStateException.class, flow::toList);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
