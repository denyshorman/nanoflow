package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class IterateWhileFlow<T> extends BufferedFlow<T> {
    public IterateWhileFlow(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
        super(new Action<>(seed, hasNext, next));
    }

    private record Action<T>(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws InterruptedException {
            var current = seed;
            while (hasNext.test(current) && !Thread.currentThread().isInterrupted()) {
                emitter.emit(current);
                current = next.apply(current);
            }
        }
    }
}
