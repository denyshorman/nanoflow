package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

public class DropFlow<T> extends BufferedFlow<T> {
    public DropFlow(Flow<? extends T> upstream, long count) {
        super(new Action<>(upstream, count));
    }

    private record Action<T>(Flow<? extends T> upstream, long count) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            try (var items = upstream.open()) {
                var remaining = count;
                for (var item : items) {
                    if (remaining > 0) {
                        remaining--;
                        continue;
                    }
                    emitter.emit(item);
                }
            }
        }
    }
}
