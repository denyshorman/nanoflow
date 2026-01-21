package io.github.denyshorman.nanoflow.generators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeverTest {
    @Test
    void shouldCompleteWhenTakenZero() {
        var values = Flows.<Integer>never().take(0).toList();

        assertEquals(List.of(), values);
    }
}
