package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

public class BufferFlow<T> extends BufferedFlow<T> {
    public BufferFlow(Flow<? extends T> upstream, int bufferSize) {
        super(bufferSize, new Action<>(upstream));
    }

    private record Action<T>(Flow<? extends T> upstream) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            try (var items = upstream.open()) {
                for (var item : items) {
                    emitter.emit(item);
                }
            }
        }
    }
}
