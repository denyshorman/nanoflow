package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        void shouldRejectNonPositiveSize() {
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(0));
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
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(1, Duration.ZERO));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).chunked(1, Duration.ofMillis(-1)));
        }
    }
}
