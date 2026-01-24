package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.HashSet;

public class DistinctFlow<T> extends BufferedFlow<T> {
    public DistinctFlow(Flow<? extends T> upstream) {
        super(new Action<>(upstream));
    }

    private record Action<T>(Flow<? extends T> upstream) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            var seen = new HashSet<T>();
            try (var items = upstream.open()) {
                for (var item : items) {
                    if (seen.add(item)) {
                        emitter.emit(item);
                    }
                }
            }
        }
    }
}
