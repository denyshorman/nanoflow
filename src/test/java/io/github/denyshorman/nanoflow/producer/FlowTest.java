package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTest {
    @Nested
    class ActionFlow {
        @Test
        void shouldCreateFlowFromAction() {
            var flow = Flows.flow(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
            });

            assertEquals(List.of(1, 2), flow.toList());
        }
    }

    @Nested
    class BufferedActionFlow {
        @Test
        void shouldCreateFlowWithBuffer() {
            var flow = Flows.flow(1, emitter -> {
                emitter.emit(1);
                emitter.emit(2);
            });

            assertEquals(List.of(1, 2), flow.toList());
        }

        @Test
        void shouldRejectNegativeBufferSize() {
            var flow = Flows.<Integer>flow(-1, emitter -> emitter.emit(1));

            assertThrows(IllegalArgumentException.class, () -> {
                try (var items = flow.open()) {
                    assertTrue(items.iterator().hasNext());
                }
            });
        }
    }
}
