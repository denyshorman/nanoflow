package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.ArrayDeque;
import java.util.List;

public class WindowedSizeFlow<T> extends BufferedFlow<List<T>> {
    public WindowedSizeFlow(Flow<? extends T> upstream, int size, int step) {
        super(new Action<>(upstream, size, step));
    }

    private record Action<T>(Flow<? extends T> upstream, int size, int step) implements Flow.Action<List<T>> {
        public void perform(Emitter<List<T>> emitter) throws Exception {
            try (var items = upstream.open()) {
                var buffer = new ArrayDeque<T>(size);
                var index = 0L;

                for (var item : items) {
                    buffer.addLast(item);

                    if (buffer.size() > size) {
                        buffer.removeFirst();
                    }

                    if (buffer.size() == size) {
                        var startIndex = index + 1 - size;

                        if (startIndex % step == 0) {
                            emitter.emit(List.copyOf(buffer));
                        }
                    }

                    index++;
                }
            }
        }
    }
}
