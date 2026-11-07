package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeTest {
    @Test
    void shouldCreateRange() {
        var values = Flows.range(1, 4).toList();

        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void shouldSupportNegativeAndZeroBounds() {
        assertEquals(List.of(-2, -1, 0, 1), Flows.range(-2, 2).toList());
    }

    @Test
    void shouldSupportMaxValueUpperBound() {
        assertEquals(
                List.of(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1),
                Flows.range(Integer.MAX_VALUE - 2, Integer.MAX_VALUE).toList()
        );
    }

    @Test
    void shouldReturnEmptyWhenStartAtOrAfterEnd() {
        assertEquals(List.of(), Flows.range(3, 3).toList());
        assertEquals(List.of(), Flows.range(4, 3).toList());
    }
}
