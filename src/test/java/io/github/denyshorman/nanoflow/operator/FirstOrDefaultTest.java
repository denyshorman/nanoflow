package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FirstOrDefaultTest {
    @Test
    void shouldReturnDefaultOnEmpty() {
        assertEquals(7, Flows.<Integer>empty().firstOrDefault(7));
    }
}
