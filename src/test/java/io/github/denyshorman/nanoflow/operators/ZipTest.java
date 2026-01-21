package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZipTest {
    @Test
    void shouldZipUntilShortestCompletes() {
        var flow = Flows.of(1, 2, 3)
                .zip(Flows.of("a", "b"), (left, right) -> left + right);

        assertEquals(List.of("1a", "2b"), flow.toList());
    }
}
