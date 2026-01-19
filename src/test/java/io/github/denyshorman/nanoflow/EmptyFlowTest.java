package io.github.denyshorman.nanoflow;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.github.denyshorman.nanoflow.Flows.emptyFlow;
import static org.junit.jupiter.api.Assertions.*;

class EmptyFlowTest {
    @Test
    void shouldNotEmitAnyValues() {
        var flow = Flows.<String>emptyFlow();
        var count = new AtomicInteger(0);
        flow.collect(value -> count.incrementAndGet());
        assertEquals(0, count.get(), "Empty flow should not emit any values");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void shouldReturnSameSingletonInstance() {
        Flow flow1 = emptyFlow();
        Flow flow2 = emptyFlow();
        assertSame(flow1, flow2, "emptyFlow() should return the same singleton instance");
    }

    @Test
    void shouldCompleteImmediately() {
        var flow = Flows.<String>emptyFlow();
        var completed = new AtomicInteger(0);
        flow.collect(value -> fail("Empty flow should not invoke collector"));
        completed.set(1);
        assertEquals(1, completed.get(), "Flow should have completed");
    }
}
