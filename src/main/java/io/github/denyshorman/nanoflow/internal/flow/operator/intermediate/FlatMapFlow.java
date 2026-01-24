package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.function.Function;

public class FlatMapFlow<T, R> extends BufferedFlow<R> {
    public FlatMapFlow(
            Flow<? extends T> upstream,
            Function<? super T, ? extends Flow<? extends R>> mapper
    ) {
        super(new Action<>(upstream, mapper));
    }

    private record Action<T, R>(
            Flow<? extends T> upstream,
            Function<? super T, ? extends Flow<? extends R>> mapper
    ) implements Flow.Action<R> {
        public void perform(Emitter<R> emitter) throws Exception {
            try (var items = upstream.open()) {
                for (var item : items) {
                    var innerFlow = mapper.apply(item);

                    try (var innerItems = innerFlow.open()) {
                        for (var innerItem : innerItems) {
                            emitter.emit(innerItem);
                        }
                    }
                }
            }
        }
    }
}
