package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

public class ConcatFlow<T> extends BufferedFlow<T> {
    public ConcatFlow(Flow<? extends T>[] flows) {
        super(new Action<>(flows));
    }

    private record Action<T>(Flow<? extends T>[] flows) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            for (var flow : flows) {
                try (var items = flow.open()) {
                    for (var item : items) {
                        emitter.emit(item);
                    }
                }
            }
        }
    }
}
