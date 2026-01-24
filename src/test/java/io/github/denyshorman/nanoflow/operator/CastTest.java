package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CastTest {
    @Test
    void shouldCastValues() {
        var flow = Flows.<Object>of("a", "b").cast(String.class);

        assertEquals(List.of("a", "b"), flow.toList());
    }

    @Test
    void shouldFailWhenTypeDoesNotMatch() {
        var flow = Flows.<Object>of("a", 1).cast(String.class);

        assertThrows(ClassCastException.class, flow::toList);
    }
}
