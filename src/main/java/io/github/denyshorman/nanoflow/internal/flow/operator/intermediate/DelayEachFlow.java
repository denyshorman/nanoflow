package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.time.Duration;

public class DelayEachFlow<T> extends BufferedFlow<T> {
    public DelayEachFlow(Flow<? extends T> upstream, Duration delay) {
        super(new Action<>(upstream, delay));
    }

    private record Action<T>(Flow<? extends T> upstream, Duration delay) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            try (var items = upstream.open()) {
                for (var item : items) {
                    if (!delay.isZero()) {
                        Thread.sleep(delay);
                    }
                    emitter.emit(item);
                }
            }
        }
    }
}
