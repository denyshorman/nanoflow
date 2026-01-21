package io.github.denyshorman.nanoflow.generators;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static io.github.denyshorman.nanoflow.Flows.interval;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntervalTest {
    @Nested
    class PeriodOnly {
        @Test
        void shouldEmitTicksWithPeriodOnly() {
            var values = interval(Duration.ofMillis(1))
                    .take(2)
                    .toList();

            assertEquals(List.of(0L, 1L), values);
        }

        @Test
        void shouldRejectNonPositivePeriod() {
            assertThrows(IllegalArgumentException.class, () -> interval(Duration.ZERO));
            assertThrows(IllegalArgumentException.class, () -> interval(Duration.ofMillis(-1)));
        }
    }

    @Nested
    class InitialDelayAndPeriod {
        @Test
        void shouldEmitTicksWithInitialDelayAndPeriod() {
            var values = interval(Duration.ZERO, Duration.ofMillis(1))
                    .take(3)
                    .toList();

            assertEquals(List.of(0L, 1L, 2L), values);
        }

        @Test
        void shouldRejectNegativeInitialDelay() {
            assertThrows(IllegalArgumentException.class, () -> interval(Duration.ofMillis(-1), Duration.ofMillis(1)));
        }

        @Test
        void shouldRejectNonPositivePeriod() {
            assertThrows(IllegalArgumentException.class, () -> interval(Duration.ZERO, Duration.ZERO));
            assertThrows(IllegalArgumentException.class, () -> interval(Duration.ofMillis(1), Duration.ofMillis(-1)));
        }
    }
}
