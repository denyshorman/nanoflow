package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ZipTest {
    @Test
    void shouldZipUntilShortestCompletes() {
        var flow = Flows.of(1, 2, 3)
                .zip(Flows.of("a", "b"), (left, right) -> left + right);

        assertEquals(List.of("1a", "2b"), flow.toList());
    }

    @Test
    void shouldZipWhenRightIsLonger() {
        var flow = Flows.of(1, 2)
                .zip(Flows.of("a", "b", "c"), (left, right) -> left + right);

        assertEquals(List.of("1a", "2b"), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenLeftIsEmpty() {
        var flow = Flows.<Integer>empty()
                .zip(Flows.of("a", "b"), (left, right) -> left + right);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldReturnEmptyWhenRightIsEmpty() {
        var flow = Flows.of(1, 2)
                .zip(Flows.<String>empty(), (left, right) -> left + right);

        assertEquals(List.of(), flow.toList());
    }

    @Test
    void shouldPropagateLeftExceptionAndCloseRight() throws InterruptedException {
        var rightClosed = new CountDownLatch(1);

        var right = Flows.<String>flow(emitter -> {
            try {
                emitter.emit("a");
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                rightClosed.countDown();
            }
        });

        var flow = Flows.<Integer>error(new IOException("left boom"))
                .zip(right, (l, r) -> l + r);

        assertThrows(IOException.class, flow::toList);
        assertTrue(rightClosed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldPropagateRightExceptionAndCloseLeft() throws InterruptedException {
        var leftClosed = new CountDownLatch(1);

        var left = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
            } finally {
                leftClosed.countDown();
            }
        });

        var flow = left.zip(Flows.<String>error(new IOException("right boom")), (l, r) -> l + r);

        assertThrows(IOException.class, flow::toList);
        assertTrue(leftClosed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldPropagateZipperExceptionAndCloseBoth() throws InterruptedException {
        var leftClosed = new CountDownLatch(1);
        var rightClosed = new CountDownLatch(1);

        var left = Flows.<Integer>flow(emitter -> {
            try {
                emitter.emit(1);
                emitter.emit(2);
            } finally {
                leftClosed.countDown();
            }
        });

        var right = Flows.<String>flow(emitter -> {
            try {
                emitter.emit("a");
                emitter.emit("b");
            } finally {
                rightClosed.countDown();
            }
        });

        var flow = left.zip(right, (l, r) -> {
            throw new IllegalStateException("zipper boom");
        });

        assertThrows(IllegalStateException.class, flow::toList);
        assertTrue(leftClosed.await(5, TimeUnit.SECONDS));
        assertTrue(rightClosed.await(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(2)
    void shouldCloseBlockingRightFlowImmediatelyWhenLeftExhausts() throws InterruptedException {
        var rightClosed = new CountDownLatch(1);

        var right = Flows.<String>flow(emitter -> {
            try {
                emitter.emit("a");
                Thread.sleep(Long.MAX_VALUE);
            } finally {
                rightClosed.countDown();
            }
        });

        var values = Flows.of(1).zip(right, (l, r) -> l + r).toList();

        assertEquals(List.of("1a"), values);
        assertTrue(rightClosed.await(5, TimeUnit.SECONDS));
    }
}
