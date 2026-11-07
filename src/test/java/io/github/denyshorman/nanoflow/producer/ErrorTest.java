package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorTest {
    @Test
    void shouldPropagateError() {
        var error = new IllegalStateException("boom");
        var flow = Flows.<Integer>error(error);

        var thrown = assertThrows(IllegalStateException.class, flow::toList);

        assertSame(error, thrown);
    }

    @Test
    void shouldPropagateCheckedExceptionAndAllowReuse() {
        var error = new IOException("disk full");
        var flow = Flows.<String>error(error);

        assertSame(error, assertThrows(IOException.class, flow::toList));
        assertSame(error, assertThrows(IOException.class, flow::toList));
    }

    @Test
    void shouldRestoreInterruptFlagWhenErrorIsInterruptedException() {
        var error = new InterruptedException("cancelled");
        var flow = Flows.<Integer>error(error);

        var thrown = assertThrows(InterruptedException.class, flow::toList);

        assertSame(error, thrown);
        assertTrue(Thread.interrupted());
    }
}
