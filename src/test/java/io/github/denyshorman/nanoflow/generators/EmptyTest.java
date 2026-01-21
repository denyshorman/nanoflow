package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmptyTest {
    @Test
    void shouldReturnEmptyFlow() {
        var values = Flows.<Integer>empty().toList();

        assertEquals(List.of(), values);
    }
}
