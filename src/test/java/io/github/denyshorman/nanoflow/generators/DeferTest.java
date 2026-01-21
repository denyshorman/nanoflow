package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeferTest {
    @Test
    void shouldDeferFlowCreationPerOpen() {
        var calls = new AtomicInteger();

        var flow = Flows.defer(() -> {
            calls.incrementAndGet();
            return Flows.of(1, 2);
        });

        assertEquals(List.of(1, 2), flow.toList());
        assertEquals(List.of(1, 2), flow.toList());
        assertEquals(2, calls.get());
    }
}
