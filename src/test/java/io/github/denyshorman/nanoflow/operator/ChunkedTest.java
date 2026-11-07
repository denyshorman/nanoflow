package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChunkedTest {
    @Nested
    class SizeBased {
        @Test
        void shouldChunkItems() {
            var flow = Flows.of(1, 2, 3, 4, 5).chunked(2);

            assertEquals(List.of(
                    List.of(1, 2),
                    List.of(3, 4),
                    List.of(5)
            ), flow.toList());
        }

        @Test
        void shouldReturnEmptyForEmptyFlow() {
            assertEquals(List.of(), Flows.<Integer>empty().chunked(3).toList());
        }

        @Test
        void shouldEmitUnmodifiableChunkSnapshots() {
            var chunk = Flows.of(1, 2).chunked(2).first();

            assertThrows(UnsupportedOperationException.class, () -> chunk.add(3));
        }

        @Test
        void shouldRejectNonPositiveSize() {
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(0));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(-1));
        }

        @Test
        void shouldPropagateUpstreamExceptionAfterCompletedChunks() {
            var received = new ArrayList<List<Integer>>();

            var flow = Flows.<Integer>flow(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
                throw new IOException("boom");
            }).chunked(2);

            assertThrows(IOException.class, () -> flow.collect(received::add));
            assertEquals(List.of(List.of(1, 2)), received);
        }
    }

    @Nested
    class TimeBased {
        @Test
        void shouldChunkItemsByTimeWindow() {
            var flow = Flows.<Integer>flow(emitter -> {
                emitter.emit(1);
                Thread.sleep(200);
                emitter.emit(2);
            });

            var values = flow.chunked(Duration.ofMillis(100)).toList();

            assertEquals(List.of(
                    List.of(1),
                    List.of(2)
            ), values);
        }

        @Test
        void shouldReturnEmptyForEmptyFlow() {
            assertEquals(List.of(), Flows.<Integer>empty().chunked(Duration.ofMillis(50)).toList());
        }

        @Test
        void shouldFlushPendingBatchBeforePropagatingUpstreamException() {
            var received = new ArrayList<List<Integer>>();

            var flow = Flows.<Integer>flow(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
                throw new IOException("boom");
            }).chunked(Duration.ofSeconds(10));

            assertThrows(IOException.class, () -> flow.collect(received::add));
            assertEquals(List.of(List.of(1, 2)), received);
        }

        @Test
        void shouldCancelUpstreamPumpWhenConsumerStopsEarly() throws InterruptedException {
            var closed = new CountDownLatch(1);

            var flow = Flows.<Integer>flow(emitter -> {
                try {
                    emitter.emit(1);
                    Thread.sleep(Long.MAX_VALUE);
                } finally {
                    closed.countDown();
                }
            }).chunked(Duration.ofMillis(20));

            assertEquals(List.of(List.of(1)), flow.take(1).toList());
            assertTrue(closed.await(5, TimeUnit.SECONDS));
        }

        @Test
        void shouldRejectNonPositiveWindow() {
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(Duration.ZERO));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(Duration.ofMillis(-1)));
        }
    }

    @Nested
    class SizeOrTimeBased {
        @Test
        void shouldChunkItemsBySizeWithMaxWait() {
            var values = Flows.of(1, 2, 3)
                    .chunked(2, Duration.ofSeconds(10))
                    .toList();

            assertEquals(List.of(
                    List.of(1, 2),
                    List.of(3)
            ), values);
        }

        @Test
        void shouldChunkItemsBySizeOrTime() {
            var flow = Flows.<Integer>flow(emitter -> {
                emitter.emit(1);
                Thread.sleep(200);
                emitter.emit(2);
                emitter.emit(3);
            });

            var values = flow.chunked(2, Duration.ofMillis(100)).toList();

            assertEquals(List.of(
                    List.of(1),
                    List.of(2, 3)
            ), values);
        }

        @Test
        void shouldRejectNonPositiveSizeOrMaxWait() {
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(0, Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(-1, Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(1, Duration.ZERO));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(1, Duration.ofMillis(-1)));
        }
    }
}
