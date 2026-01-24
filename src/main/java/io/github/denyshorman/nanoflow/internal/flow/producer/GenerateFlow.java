package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.function.Supplier;

public class GenerateFlow<T> extends BufferedFlow<T> {
    public GenerateFlow(Supplier<? extends T> supplier) {
        super(new Action<>(supplier));
    }

    private record Action<T>(Supplier<? extends T> supplier) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws InterruptedException {
            while (!Thread.currentThread().isInterrupted()) {
                emitter.emit(supplier.get());
            }
        }
    }
}
