package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FirstOrDefaultTest {
    @Test
    void shouldReturnFirstItemWhenPresent() {
        assertEquals(1, Flows.of(1, 2, 3).firstOrDefault(7));
    }

    @Test
    void shouldReturnDefaultOnEmpty() {
        assertEquals(7, Flows.<Integer>empty().firstOrDefault(7));
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).firstOrDefault(7));
    }

    @Test
    @Timeout(2)
    void shouldCancelUpstreamImmediatelyAfterFirstItem() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                closed.countDown();
            }
        });

        assertEquals(1, flow.firstOrDefault(7));
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
