package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MapTest {
    @Test
    void shouldMapValues() {
        var flow = Flows.of(1, 2, 3).map(value -> value * 2);

        assertEquals(List.of(2, 4, 6), flow.toList());
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        var flow = Flows.<Integer>empty().map(value -> value * 2);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldMapToDifferentType() {
        var flow = Flows.of(1, 2, 3).map( value -> "v" + value);

        assertEquals(List.of("v1", "v2", "v3"), flow.toList());
    }

    @Test
    void shouldBeReusableAcrossMultipleCollections() {
        var flow = Flows.of(1, 2).map(value -> value + 10);

        assertEquals(List.of(11, 12), flow.toList());
        assertEquals(List.of(11, 12), flow.toList());
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.<Integer>error(new IOException("upstream boom"))
                .map(value -> value * 2);

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldPropagateMapperExceptionAndCancelUpstream() throws InterruptedException {
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
            } finally {
                closed.countDown();
            }
        }).map(value -> {
            if (value == 2) {
                throw new IllegalStateException("mapper boom");
            }
            return value * 10;
        });

        assertThrows(IllegalStateException.class, flow::toList);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldLazilyTransformOnlyConsumedElementsWhenCancelledEarly() throws InterruptedException {
        var mapperCalls = new AtomicInteger();
        var closed = new CountDownLatch(1);

        var flow = Flows.<Integer>flow(emitter -> {
            try {
                var i = 1;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++);
                }
            } finally {
                closed.countDown();
            }
        }).map(value -> {
            mapperCalls.incrementAndGet();
            return value * 10;
        });

        assertEquals(List.of(10, 20), flow.take(2).toList());
        assertEquals(2, mapperCalls.get());
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
