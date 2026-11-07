package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DistinctUntilChangedTest {
    @Test
    void shouldSkipAdjacentDuplicates() {
        var flow = Flows.of(1, 1, 2, 2, 1).distinctUntilChanged();

        assertEquals(List.of(1, 2, 1), flow.toList());
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        assertEquals(List.of(), Flows.<Integer>empty().distinctUntilChanged().toList());
    }

    @Test
    void shouldEmitSingleValueWhenAllElementsAreEqual() {
        var flow = Flows.of("a", "a", "a").distinctUntilChanged();

        assertEquals(List.of("a"), flow.toList());
    }

    @Test
    void shouldUseValueEqualityNotReferenceIdentity() {
        record Box(int id) {
        }

        var flow = Flows.of(new Box(1), new Box(1), new Box(2))
                .distinctUntilChanged();

        assertEquals(List.of(new Box(1), new Box(2)), flow.toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).distinctUntilChanged();

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldCancelInfiniteUpstreamWhenDownstreamStopsEarly() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                var i = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++ / 3);
                }
            } finally {
                closed.countDown();
            }
        }).distinctUntilChanged();

        assertEquals(List.of(0, 1, 2), flow.take(3).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
