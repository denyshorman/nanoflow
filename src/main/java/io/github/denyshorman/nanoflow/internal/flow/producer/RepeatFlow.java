package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

public class RepeatFlow<T> extends BufferedFlow<T> {
    public RepeatFlow(T value) {
        super(new Action<>(value));
    }

    private record Action<T>(T value) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws InterruptedException {
            while (!Thread.currentThread().isInterrupted()) {
                emitter.emit(value);
            }
        }
    }
}
