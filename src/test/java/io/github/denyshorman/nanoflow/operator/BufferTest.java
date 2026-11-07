package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BufferTest {
    @Test
    void shouldPreserveValues() {
        var flow = Flows.of(1, 2, 3).buffer(1);

        assertEquals(List.of(1, 2, 3), flow.toList());
    }

    @Test
    void shouldSupportZeroAndUnboundedBufferSizes() {
        assertEquals(List.of(1, 2, 3), Flows.of(1, 2, 3).buffer(0).toList());
        assertEquals(List.of(1, 2, 3), Flows.of(1, 2, 3).buffer(Integer.MAX_VALUE).toList());
    }

    @Test
    void shouldRejectNegativeBufferSize() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).buffer(-1));
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("boom")).buffer(2);

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldAllowProducerToBufferUpToCapacityAndBlockWhenFull() throws InterruptedException {
        var buffered = new CountDownLatch(1);
        var fourthEmitted = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            buffered.countDown();
            emitter.emit(4);
            fourthEmitted.countDown();
        }).buffer(2);

        try (var items = flow.open()) {
            assertTrue(buffered.await(5, TimeUnit.SECONDS));
            assertFalse(fourthEmitted.await(20, TimeUnit.MILLISECONDS));

            var iterator = items.iterator();
            assertTrue(iterator.hasNext());
            assertEquals(1, iterator.next());

            assertTrue(fourthEmitted.await(5, TimeUnit.SECONDS));
            assertTrue(iterator.hasNext());
            assertEquals(2, iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals(3, iterator.next());
            assertTrue(iterator.hasNext());
            assertEquals(4, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    void shouldCancelUpstreamWhenConsumerClosesEarly() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                var i = 1;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++);
                }
            } finally {
                closed.countDown();
            }
        }).buffer(4);

        assertEquals(List.of(1, 2), flow.take(2).toList());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
