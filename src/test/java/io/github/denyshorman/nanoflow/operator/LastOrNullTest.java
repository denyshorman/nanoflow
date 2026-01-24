package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LastOrNullTest {
    @Test
    void shouldReturnLastItem() {
        var flow = Flows.of(1, 2, 3);

        assertEquals(3, flow.lastOrNull());
    }

    @Test
    void shouldReturnNullOnEmpty() {
        assertNull(Flows.<Integer>empty().lastOrNull());
    }
}
