package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.function.Predicate;

public class FilterFlow<T> extends BufferedFlow<T> {
    public FilterFlow(
            Flow<? extends T> upstream,
            Predicate<? super T> predicate,
            boolean acceptWhenTrue
    ) {
        super(new Action<>(upstream, predicate, acceptWhenTrue));
    }

    private record Action<T>(
            Flow<? extends T> upstream,
            Predicate<? super T> predicate,
            boolean acceptWhenTrue
    ) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            try (var items = upstream.open()) {
                for (var item : items) {
                    if (acceptWhenTrue == predicate.test(item)) {
                        emitter.emit(item);
                    }
                }
            }
        }
    }
}
