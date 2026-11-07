package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AnyTest {
    @Test
    void shouldReturnTrueWhenPredicateMatches() {
        assertTrue(Flows.of(1, 2, 3).any(value -> value == 2));
    }

    @Test
    void shouldReturnFalseWhenPredicateDoesNotMatch() {
        assertFalse(Flows.of(1, 2, 3).any(value -> value == 5));
    }

    @Test
    void shouldReturnFalseOnEmptyFlow() {
        assertFalse(Flows.<Integer>empty().any(value -> true));
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).any(value -> true));
    }

    @Test
    @Timeout(2)
    void shouldShortCircuitAndCancelUpstreamOnFirstMatch() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        });

        assertTrue(flow.any(value -> value == 2));
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
