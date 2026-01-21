package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectTest {
    @Test
    void shouldCollectValues() {
        var list = new ArrayList<Integer>();

        Flows.of(1, 2, 3).collect(list::add);

        assertEquals(List.of(1, 2, 3), list);
    }

    @Test
    void shouldPropagateCollectorException() {
        var flow = Flows.of(1, 2);

        assertThrows(IOException.class, () -> flow.collect(value -> {
            if (value == 2) {
                throw new IOException("boom");
            }
        }));
    }
}
