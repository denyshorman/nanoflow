package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

public class RepeatCountFlow<T> extends BufferedFlow<T> {
    public RepeatCountFlow(long count, T value) {
        super(new Action<>(count, value));
    }

    private record Action<T>(long count, T value) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws InterruptedException {
            for (var i = 0L; i < count; i++) {
                emitter.emit(value);
            }
        }
    }
}
