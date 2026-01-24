package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.ArrayList;
import java.util.List;

public class ChunkedFlow<T> extends BufferedFlow<List<T>> {
    public ChunkedFlow(Flow<? extends T> upstream, int size) {
        super(new Action<>(upstream, size));
    }

    private record Action<T>(Flow<? extends T> upstream, int size) implements Flow.Action<List<T>> {
        public void perform(Emitter<List<T>> emitter) throws Exception {
            try (var items = upstream.open()) {
                var chunk = new ArrayList<T>(size);

                for (var item : items) {
                    chunk.add(item);

                    if (chunk.size() == size) {
                        emitter.emit(List.copyOf(chunk));
                        chunk.clear();
                    }
                }

                if (!chunk.isEmpty()) {
                    emitter.emit(List.copyOf(chunk));
                }
            }
        }
    }
}
