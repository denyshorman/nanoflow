package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ConcatTest {
    @Test
    void shouldAppendOtherFlow() {
        var flow = Flows.of(1, 2).concat(Flows.of(3, 4));

        assertEquals(List.of(1, 2, 3, 4), flow.toList());
    }

    @Test
    void shouldHandleEmptyFirstFlow() {
        var flow = Flows.<Integer>empty().concat(Flows.of(1, 2));

        assertEquals(List.of(1, 2), flow.toList());
    }

    @Test
    void shouldHandleEmptySecondFlow() {
        var flow = Flows.of(1, 2).concat(Flows.empty());

        assertEquals(List.of(1, 2), flow.toList());
    }

    @Test
    void shouldHandleBothFlowsEmpty() {
        var flow = Flows.<Integer>empty().concat(Flows.empty());

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldNotOpenSecondFlowWhenFirstFails() {
        var secondOpened = new AtomicBoolean(false);

        var second = Flows.defer(() -> {
            secondOpened.set(true);
            return Flows.of(1);
        });

        var flow = Flows.<Integer>error(new IOException("first boom")).concat(second);

        assertThrows(IOException.class, flow::toList);
        assertFalse(secondOpened.get());
    }

    @Test
    void shouldNotOpenSecondFlowWhenConsumerStopsDuringFirstFlow() throws InterruptedException {
        var firstClosed = new CountDownLatch(1);
        var secondOpened = new AtomicBoolean(false);

        var first = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
            } finally {
                firstClosed.countDown();
            }
        });

        var second = Flows.defer(() -> {
            secondOpened.set(true);
            return Flows.of(4, 5);
        });

        assertEquals(List.of(1, 2), first.concat(second).take(2).toList());
        assertTrue(firstClosed.await(5, TimeUnit.SECONDS));
        assertFalse(secondOpened.get());
    }

    @Test
    void shouldCancelSecondFlowWhenConsumerStopsMidSecondFlow() throws InterruptedException {
        var secondClosed = new CountDownLatch(1);

        var second = Flows.<Integer>flow(emitter -> {
            try {
                var i = 3;
                while (!Thread.currentThread().isInterrupted()) {
                    emitter.emit(i++);
                }
            } finally {
                secondClosed.countDown();
            }
        });

        var flow = Flows.of(1, 2).concat(second);

        assertEquals(List.of(1, 2, 3), flow.take(3).toList());
        assertTrue(secondClosed.await(5, TimeUnit.SECONDS));
    }
}
