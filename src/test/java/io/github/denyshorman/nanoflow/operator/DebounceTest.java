package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DebounceTest {
    @Test
    void shouldDebounceByTimeout() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            Thread.sleep(50);
            emitter.emit(2);
            Thread.sleep(200);
            emitter.emit(3);
        });

        var values = flow.debounce(Duration.ofMillis(100)).toList();

        assertEquals(List.of(2, 3), values);
    }

    @Test
    void shouldEmitLastValueOnCompletion() {
        var values = Flows.of(1, 2, 3)
                .debounce(Duration.ofSeconds(1))
                .toList();

        assertEquals(List.of(3), values);
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).debounce(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).debounce(Duration.ofMillis(-1)));
    }
}
