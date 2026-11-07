package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SingleTest {
    @Test
    void shouldReturnSingleItem() {
        assertEquals(1, Flows.of(1).single());
    }

    @Test
    void shouldThrowOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> Flows.<Integer>empty().single());
    }

    @Test
    void shouldThrowOnMultipleItems() {
        assertThrows(IllegalStateException.class, () -> Flows.of(1, 2).single());
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).single());
    }

    @Test
    @Timeout(2)
    void shouldCancelUpstreamImmediatelyOnSecondItem() throws InterruptedException {
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

        assertThrows(IllegalStateException.class, flow::single);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
