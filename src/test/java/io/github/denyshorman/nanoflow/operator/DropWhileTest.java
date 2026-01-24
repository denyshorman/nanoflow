package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DropWhileTest {
    @Test
    void shouldDropWhilePredicateMatches() {
        var flow = Flows.of(1, 2, 3, 1).dropWhile(value -> value < 3);

        assertEquals(List.of(3, 1), flow.toList());
    }
}
