package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SampleTest {
    @Test
    void shouldSampleLatestValues() {
        var flow = Flows.<Integer>flow(emitter -> {
            emitter.emit(1);
            Thread.sleep(50);
            emitter.emit(2);
            Thread.sleep(160);
            emitter.emit(3);
            Thread.sleep(160);
            emitter.emit(4);
        });

        var values = flow.sample(Duration.ofMillis(100)).toList();

        assertEquals(List.of(2, 3, 4), values);
    }

    @Test
    void shouldEmitLastValueOnCompletion() {
        var values = Flows.of(1, 2, 3)
                .sample(Duration.ofSeconds(1))
                .toList();

        assertEquals(List.of(3), values);
    }

    @Test
    void shouldRejectNonPositivePeriod() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).sample(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).sample(Duration.ofMillis(-1)));
    }
}
