package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErrorTest {
    @Test
    void shouldPropagateError() {
        var error = new IllegalStateException("boom");
        var flow = Flows.<Integer>error(error);

        var thrown = assertThrows(IllegalStateException.class, flow::toList);

        assertSame(error, thrown);
    }
}
