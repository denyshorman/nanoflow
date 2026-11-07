package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CollectTest {
    @Test
    void shouldCollectValues() {
        var list = new ArrayList<Integer>();

        Flows.of(1, 2, 3).collect(list::add);

        assertEquals(List.of(1, 2, 3), list);
    }

    @Test
    void shouldPropagateCollectorException() {
        var flow = Flows.of(1, 2);

        assertThrows(IOException.class, () -> flow.collect(value -> {
            if (value == 2) {
                throw new IOException("boom");
            }
        }));
    }

    @Test
    void shouldPropagateUpstreamCheckedException() {
        var flow = Flows.<Integer>error(new IOException("producer boom"));

        assertThrows(IOException.class, () -> flow.collect(value -> {
        }));
    }

    @Test
    void shouldCancelUpstreamWhenCollectorThrows() throws InterruptedException {
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

        assertThrows(IOException.class, () -> flow.collect(value -> {
            if (value == 2) {
                throw new IOException("collector boom");
            }
        }));
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
