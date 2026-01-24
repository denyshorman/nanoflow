package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.function.BiFunction;

public class ScanFlow<T, R> extends BufferedFlow<R> {
    public ScanFlow(
            Flow<? extends T> upstream,
            R initial,
            BiFunction<? super R, ? super T, ? extends R> reducer
    ) {
        super(new Action<>(upstream, initial, reducer));
    }

    private record Action<T, R>(
            Flow<? extends T> upstream,
            R initial,
            BiFunction<? super R, ? super T, ? extends R> reducer
    ) implements Flow.Action<R> {
        public void perform(Emitter<R> emitter) throws Exception {
            var accumulator = initial;
            emitter.emit(accumulator);

            try (var items = upstream.open()) {
                for (var item : items) {
                    accumulator = reducer.apply(accumulator, item);
                    emitter.emit(accumulator);
                }
            }
        }
    }
}
