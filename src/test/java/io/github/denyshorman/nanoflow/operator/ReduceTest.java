package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ReduceTest {
    @Test
    void shouldReduceValues() {
        assertEquals(6, Flows.of(1, 2, 3).reduce(Integer::sum));
    }

    @Test
    void shouldReturnSingleElementWithoutCallingReducer() {
        var called = new AtomicBoolean(false);

        var result = Flows.of(42).reduce((a, b) -> {
            called.set(true);
            return a + b;
        });

        assertEquals(42, result);
        assertFalse(called.get());
    }

    @Test
    void shouldThrowOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> Flows.<Integer>empty().reduce(Integer::sum));
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).reduce(Integer::sum));
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
        });

        assertThrows(IllegalStateException.class, () -> flow.reduce((a, b) -> {
            throw new IllegalStateException("reducer boom");
        }));
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
