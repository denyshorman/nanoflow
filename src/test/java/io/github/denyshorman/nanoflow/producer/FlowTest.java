package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

        @Test
        void shouldCompleteWhenProducerRestoresInterruptFlagAndReturns() {
            var flow = Flows.<Integer>flow(emitter -> {
                emitter.emit(1);
                Thread.currentThread().interrupt();
            });

            assertEquals(List.of(1), flow.toList());
        }

        @Test
        void shouldPropagateExceptionWhenProducerRestoresInterruptFlagAndThrows() {
            var error = new InterruptedException("interrupted");

            var flow = Flows.<Integer>flow(emitter -> {
                emitter.emit(1);
                Thread.currentThread().interrupt();
                throw error;
            });

            var thrown = assertThrows(
                    InterruptedException.class,
                    () -> flow.filter(x -> true).sample(Duration.ofMillis(50)).toList()
            );

            assertSame(error, thrown);
            assertTrue(Thread.interrupted());
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

        @Test
        void shouldEmitAllValuesConcurrently() {
            var count = 100000;
            var latch = new CountDownLatch(count);

            var flow = Flows.<Integer>flow(emitter -> {
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    for (var i = 0; i <= count; i++) {
                        final var value = i;

                        executor.submit(() -> {
                            try {
                                latch.countDown();
                                latch.await();
                                emitter.emit(value);
                            } catch (InterruptedException ignored) {
                            }
                        });
                    }
                }
            });

            var sum = 0L;

            try (var numbers = flow.open()) {
                for (var number : numbers) {
                    sum += number;
                }
            }

            assertEquals(count*(count + 1L)/2, sum);
        }
    }
}
