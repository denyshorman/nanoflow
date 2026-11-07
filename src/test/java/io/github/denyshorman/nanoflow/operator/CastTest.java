package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CastTest {
    @Test
    void shouldCastValues() {
        var flow = Flows.<Object>of("a", "b").cast(String.class);

        assertEquals(List.of("a", "b"), flow.toList());
    }

    @Test
    void shouldReturnEmptyForEmptyFlow() {
        var flow = Flows.empty().cast(String.class);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldCastToSupertypeOrInterface() {
        var flow = Flows.of("hello", "world").cast(CharSequence.class);

        assertEquals(List.of("hello", "world"), flow.toList());
    }

    @Test
    void shouldFailWhenTypeDoesNotMatch() {
        var flow = Flows.<Object>of("a", 1).cast(String.class);

        assertThrows(ClassCastException.class, flow::toList);
    }

    @Test
    void shouldPropagateUpstreamException() {
        var flow = Flows.error(new IOException("boom")).cast(String.class);

        assertThrows(IOException.class, flow::toList);
    }

    @Test
    void shouldEmitValidPrefixAndCancelUpstreamWhenCastFailsMidStream() throws InterruptedException {
        var closed = new CountDownLatch(1);
        var received = new ArrayList<String>();

        var flow = Flows.flow(emitter -> {
            try {
                emitter.emit("first");
                emitter.emit(42);
                emitter.emit("unreachable");
            } finally {
                closed.countDown();
            }
        }).cast(String.class);

        assertThrows(ClassCastException.class, () -> flow.collect(received::add));
        assertEquals(List.of("first"), received);
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }
}
