package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AllTest {
    @Test
    void shouldReturnTrueWhenAllMatch() {
        assertTrue(Flows.of(1, 2, 3).all(value -> value > 0));
    }

    @Test
    void shouldReturnFalseWhenAnyDoesNotMatch() {
        assertFalse(Flows.of(1, 2, 3).all(value -> value < 3));
    }

    @Test
    void shouldReturnTrueOnEmptyFlow() {
        assertTrue(Flows.<Integer>empty().all(value -> false));
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).all(value -> true));
    }

    @Test
    @Timeout(2)
    void shouldShortCircuitAndCancelUpstreamOnFirstMismatch() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(2);
                emitter.emit(3);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        });

        assertFalse(flow.all(value -> value % 2 == 0));
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
