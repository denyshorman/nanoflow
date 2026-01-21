package io.github.denyshorman.nanoflow.operators;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ToCollectionTest {
    @Test
    void shouldCollectToProvidedCollection() {
        var collection = Flows.of(1, 2, 3).toCollection(ArrayDeque::new);

        assertInstanceOf(ArrayDeque.class, collection);
        assertEquals(3, collection.size());
    }
}
