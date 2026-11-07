package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class ToCollectionTest {
    @Test
    void shouldCollectToProvidedCollection() {
        var collection = Flows.of(1, 2, 3).toCollection(ArrayDeque::new);

        assertInstanceOf(ArrayDeque.class, collection);
        assertEquals(3, collection.size());
    }

    @Test
    void shouldSupportSortedCollections() {
        var sorted = Flows.of(3, 1, 2).toCollection(TreeSet::new);

        assertEquals(List.of(1, 2, 3), List.copyOf(sorted));
    }

    @Test
    void shouldPropagateUpstreamException() {
        assertThrows(IOException.class, () -> Flows.<Integer>error(new IOException("boom")).toCollection(ArrayDeque::new));
    }
}
