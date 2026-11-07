package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FlatMapTest {
    @Test
    void shouldFlattenInnerFlows() {
        var flow = Flows.of(1, 2).flatMap(value -> Flows.of(value, value * 10));

        assertEquals(List.of(1, 10, 2, 20), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenOuterIsEmpty() {
        var flow = Flows.<Integer>empty().flatMap(value -> Flows.of(value, value * 10));

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldSkipEmptyInnerFlows() {
        var flow = Flows.of(1, 2, 3)
                .flatMap(value -> value == 2 ? Flows.empty() : Flows.of(value));

        assertEquals(List.of(1, 3), flow.toList());
    }

    @Test
    void shouldPropagateOuterException() {
        var flow = Flows.<Integer>error(new IOException("outer boom"))
                .flatMap(Flows::of);

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldPropagateMapperExceptionAndCancelOuter() throws InterruptedException {
        var outerClosed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
            } finally {
                outerClosed.countDown();
            }
        }).flatMap(value -> {
            if (value == 2) {
                throw new IllegalStateException("mapper boom");
            }
            return Flows.of(value);
        });

        assertThrows(IllegalStateException.class, flow::toList);
        assertTrue(outerClosed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldPropagateInnerExceptionAndCancelOuter() throws InterruptedException {
        var outerClosed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
            } finally {
                outerClosed.countDown();
            }
        }).flatMap(value -> value == 2
                ? Flows.error(new IOException("inner boom"))
                : Flows.of(value));

        assertThrows(IOException.class, flow::toList);
        assertTrue(outerClosed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldCancelActiveInnerAndOuterFlowsWhenDownstreamBreaksEarly() throws InterruptedException {
        var outerClosed = new CountDownLatch(1);
        var innerClosed = new CountDownLatch(1);
        var secondInnerOpened = new AtomicBoolean(false);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
            } finally {
                outerClosed.countDown();
            }
        }).flatMap(value -> {
            if (value == 2) {
                secondInnerOpened.set(true);
            }
            return Flows.flow(emitter -> {
                try {
                    var i = 1;
                    while (!Thread.currentThread().isInterrupted()) {
                        emitter.emit(value * 10 + i++);
                    }
                } finally {
                    innerClosed.countDown();
                }
            });
        });

        assertEquals(List.of(11, 12), flow.take(2).toList());
        assertTrue(innerClosed.await(5, TimeUnit.SECONDS));
        assertTrue(outerClosed.await(5, TimeUnit.SECONDS));
        assertFalse(secondInnerOpened.get());
    }

    @Test
    void shouldEvaluateInnerFlowsStrictlySequentially() {
        var events = new ArrayList<String>();

        var flow = Flows.of(1, 2).flatMap(value -> Flows.flow(emitter -> {
            events.add("start-" + value);
            emitter.emit(value);
            events.add("end-" + value);
        }));

        assertEquals(List.of(1, 2), flow.toList());
        assertEquals(List.of("start-1", "end-1", "start-2", "end-2"), events);
    }
}
