package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.function.Supplier;

public class GenerateCountFlow<T> extends BufferedFlow<T> {
    public GenerateCountFlow(long count, Supplier<? extends T> supplier) {
        super(new Action<>(count, supplier));
    }

    private record Action<T>(long count, Supplier<? extends T> supplier) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws InterruptedException {
            for (var i = 0L; i < count; i++) {
                emitter.emit(supplier.get());
            }
        }
    }
}
