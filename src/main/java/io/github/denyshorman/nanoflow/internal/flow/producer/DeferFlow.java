package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.function.Supplier;

public class DeferFlow<T> extends BufferedFlow<T> {
    public DeferFlow(Supplier<? extends Flow<? extends T>> supplier) {
        super(new Action<>(supplier));
    }

    private record Action<T>(Supplier<? extends Flow<? extends T>> supplier) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            try (var items = supplier.get().open()) {
                for (var item : items) {
                    emitter.emit(item);
                }
            }
        }
    }
}
