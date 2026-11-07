package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FoldTest {
    @Test
    void shouldFoldValues() {
        var result = Flows.of(1, 2, 3).fold(10, Integer::sum);

        assertEquals(16, result);
    }

    @Test
    void shouldReturnInitialOnEmptyFlow() {
        assertEquals(10, Flows.<Integer>empty().fold(10, Integer::sum));
    }

    @Test
    void shouldFoldIntoDifferentType() {
        var result = Flows.of(1, 2, 3).fold("", (acc, n) -> acc + n);

        assertEquals("123", result);
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).fold(0, Integer::sum));
    }

    @Test
    void shouldPropagateAccumulatorExceptionAndCancelUpstream() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
            } finally {
                closed.countDown();
            }
        });

        assertThrows(IllegalStateException.class, () -> flow.fold(0, (acc, n) -> {
            throw new IllegalStateException("acc boom");
        }));
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
