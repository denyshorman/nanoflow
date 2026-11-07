package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FirstOrNullTest {
    @Test
    void shouldReturnFirstItem() {
        var flow = Flows.of(1, 2, 3);

        assertEquals(1, flow.firstOrNull());
    }

    @Test
    void shouldReturnNullOnEmpty() {
        assertNull(Flows.<Integer>empty().firstOrNull());
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).firstOrNull());
    }

    @Test
    @Timeout(2)
    void shouldCancelUpstreamImmediatelyAfterFirstItem() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(10);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        });

        assertEquals(10, flow.firstOrNull());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
