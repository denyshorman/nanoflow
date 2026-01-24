package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DropTest {
    @Test
    void shouldDropLeadingItems() {
        var flow = Flows.of(1, 2, 3).drop(2);

        assertEquals(List.of(3), flow.toList());
    }

    @Test
    void shouldReturnAllWhenDroppingZero() {
        var flow = Flows.of(1, 2, 3).drop(0);

        assertEquals(List.of(1, 2, 3), flow.toList());
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Flows.of(1).drop(-1));
    }
}
