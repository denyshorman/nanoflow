package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NoneTest {
    @Test
    void shouldReturnTrueWhenNoneMatch() {
        assertTrue(Flows.of(1, 2, 3).none(value -> value < 0));
    }

    @Test
    void shouldReturnFalseWhenAnyMatches() {
        assertFalse(Flows.of(1, 2, 3).none(value -> value == 2));
    }

    @Test
    void shouldReturnTrueOnEmptyFlow() {
        assertTrue(Flows.<Integer>empty().none(value -> true));
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).none(value -> false));
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

        assertFalse(flow.none(value -> value == 2));
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
