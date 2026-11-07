package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DeferTest {
    @Test
    void shouldDeferFlowCreationPerOpen() {
        var calls = new AtomicInteger();

        var flow = Flows.defer(() -> {
            calls.incrementAndGet();
            return Flows.of(1, 2);
        });

        assertEquals(0, calls.get());
        assertEquals(List.of(1, 2), flow.toList());
        assertEquals(List.of(1, 2), flow.toList());
        assertEquals(2, calls.get());
    }

    @Test
    void shouldPropagateSupplierException() {
        var flow = Flows.<Integer>defer(() -> {
            throw new IllegalStateException("supplier boom");
        });

        assertThrows(IllegalStateException.class, flow::toList);
    }

    @Test
    void shouldPropagateInnerFlowException() {
        var flow = Flows.<Integer>defer(() -> Flows.error(new IOException("inner boom")));

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldCancelInnerFlowWhenConsumerStopsEarly() throws InterruptedException {
        var innerClosed = new CountDownLatch(1);

        var flow = Flows.defer(() -> Flows.<Integer>flow(emitter -> {
            try {
                var i = 1;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++);
                }
            } finally {
                innerClosed.countDown();
            }
        }));

        assertEquals(List.of(1, 2), flow.take(2).toList());
        assertTrue(innerClosed.await(5, TimeUnit.SECONDS));
    }
}
