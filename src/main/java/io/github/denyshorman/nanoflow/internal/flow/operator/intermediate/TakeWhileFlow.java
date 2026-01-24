package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.function.Predicate;

public class TakeWhileFlow<T> extends BufferedFlow<T> {
    public TakeWhileFlow(Flow<? extends T> upstream, Predicate<? super T> predicate) {
        super(new Action<>(upstream, predicate));
    }

    private record Action<T>(Flow<? extends T> upstream, Predicate<? super T> predicate) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            try (var items = upstream.open()) {
                for (var item : items) {
                    if (!predicate.test(item)) {
                        break;
                    }
                    emitter.emit(item);
                }
            }
        }
    }
}
