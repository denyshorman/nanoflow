package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

public class ConcatFlow<T> extends BufferedFlow<T> {
    public ConcatFlow(Flow<? extends T> upstream, Flow<? extends T> other) {
        super(new Action<>(upstream, other));
    }

    private record Action<T>(Flow<? extends T> upstream, Flow<? extends T> other) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            try (var items = upstream.open()) {
                for (var item : items) {
                    emitter.emit(item);
                }
            }

            try (var items = other.open()) {
                for (var item : items) {
                    emitter.emit(item);
                }
            }
        }
    }
}
