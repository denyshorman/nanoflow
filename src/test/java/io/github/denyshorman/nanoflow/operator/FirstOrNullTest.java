package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FirstOrNullTest {
    @Test
    void shouldReturnFirstItem() {
        var flow = Flows.of(1, 2, 3);

        assertEquals(1, flow.firstOrNull());
    }

    @Test
    void shouldReturnNullOnEmpty() {
        assertNull(Flows.<Integer>empty().firstOrNull());
    }
}
