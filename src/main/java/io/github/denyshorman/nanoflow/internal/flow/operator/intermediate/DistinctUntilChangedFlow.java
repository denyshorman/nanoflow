package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.Objects;

public class DistinctUntilChangedFlow<T> extends BufferedFlow<T> {
    public DistinctUntilChangedFlow(Flow<? extends T> upstream) {
        super(new Action<>(upstream));
    }

    private record Action<T>(Flow<? extends T> upstream) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            var last = (Object) null;
            try (var items = upstream.open()) {
                for (var item : items) {
                    if (!Objects.equals(item, last)) {
                        emitter.emit(item);
                        last = item;
                    }
                }
            }
        }
    }
}
