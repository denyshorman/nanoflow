package io.github.denyshorman.nanoflow.operator;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.Flows;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class StreamTest {
    @Test
    void shouldExposeStreamView() {
        try (var stream = Flows.of(1, 2, 3).stream()) {
            assertEquals(List.of(1, 2, 3), stream.toList());
        }
    }

    @Test
    void shouldCloseSequenceWhenIteratorThrows() {
        var closed = new AtomicBoolean(false);

        Flow<Integer> flow = () -> new Flow.Sequence<>() {
            @Override
            public Iterator<Integer> iterator() {
                throw new IllegalStateException("boom");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };

        assertThrows(IllegalStateException.class, () -> {
            try (var stream = flow.stream()) {
                stream.toList();
            }
        });

        assertTrue(closed.get());
    }
}
