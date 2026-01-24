package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeoutTest {
    @Test
    void shouldPassWhenValuesArrive() {
        var values = Flows.of(1, 2)
                .timeout(Duration.ofSeconds(30))
                .toList();

        assertEquals(List.of(1, 2), values);
    }

    @Test
    void shouldTimeoutWhenNoValueArrives() {
        assertThrows(TimeoutException.class, () -> Flows.<Integer>never().timeout(Duration.ofMillis(10)).toList());
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).timeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).timeout(Duration.ofMillis(-1)));
    }
}
