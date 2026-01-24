package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowedTest {
    @Nested
    class SizeBased {
        @Test
        void shouldWindowItems() {
            var values = Flows.of(1, 2, 3, 4, 5)
                    .windowed(3, 1)
                    .toList();

            assertEquals(List.of(
                    List.of(1, 2, 3),
                    List.of(2, 3, 4),
                    List.of(3, 4, 5)
            ), values);
        }

        @Test
        void shouldWindowItemsWithStep() {
            var values = Flows.of(1, 2, 3, 4, 5)
                    .windowed(3, 2)
                    .toList();

            assertEquals(List.of(
                    List.of(1, 2, 3),
                    List.of(3, 4, 5)
            ), values);
        }

        @Test
        void shouldRejectNonPositiveSizeOrStep() {
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).windowed(0, 1));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).windowed(1, 0));
        }
    }

    @Nested
    class TimeBased {
        @Test
        void shouldWindowItemsByTimeAndStep() {
            var flow = Flows.<Integer>flow(emitter -> {
                emitter.emit(1);
                Thread.sleep(120);
                emitter.emit(2);
                Thread.sleep(120);
                emitter.emit(3);
            });

            var values = flow.windowed(Duration.ofMillis(200), Duration.ofMillis(100)).toList();

            assertEquals(List.of(
                    List.of(1, 2),
                    List.of(2, 3),
                    List.of(3)
            ), values);
        }

        @Test
        void shouldRejectNonPositiveWindowOrStep() {
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).windowed(Duration.ZERO, Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).windowed(Duration.ofSeconds(1), Duration.ZERO));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).windowed(Duration.ofMillis(-1), Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> Flows.of(1).windowed(Duration.ofSeconds(1), Duration.ofMillis(-1)));
        }
    }
}
