package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LastOrDefaultTest {
    @Test
    void shouldReturnDefaultOnEmpty() {
        assertEquals(9, Flows.<Integer>empty().lastOrDefault(9));
    }
}
