package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.function.UnaryOperator;

public class IterateFlow<T> extends BufferedFlow<T> {
    public IterateFlow(T seed, UnaryOperator<T> next) {
        super(new Action<>(seed, next));
    }

    private record Action<T>(T seed, UnaryOperator<T> next) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws InterruptedException {
            var current = seed;
            while (!Thread.currentThread().isInterrupted()) {
                emitter.emit(current);
                current = next.apply(current);
            }
        }
    }
}
