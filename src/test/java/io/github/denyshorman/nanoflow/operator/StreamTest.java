package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.Flows;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class StreamTest {
    @Test
    void shouldExposeStreamView() {
        try (var stream = Flows.of(1, 2, 3).stream()) {
            assertEquals(List.of(1, 2, 3), stream.toList());
        }
    }

    @Test
    void shouldCancelUpstreamWhenStreamIsClosedEarly() throws InterruptedException {
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
        });

        try (var stream = flow.stream()) {
            assertEquals(List.of(1, 2), stream.limit(2).toList());
        }

        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldCloseSequenceWhenIteratorThrows() {
        var closed = new AtomicBoolean(false);

        Flow<Integer> flow = () -> new Flow.Sequence<>() {
            @Override
            public Iterator<Integer> iterator() {
                throw new IllegalStateException("boom");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };

        assertThrows(IllegalStateException.class, () -> {
            try (var stream = flow.stream()) {
                stream.toList();
            }
        });

        assertTrue(closed.get());
    }
}
