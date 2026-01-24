package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class ErrorFlow<T> extends BufferedFlow<T> {
    public ErrorFlow(Throwable error) {
        super(new Action<>(error));
    }

    private record Action<T>(Throwable error) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) {
            throw sneakyThrow(error);
        }
    }
}
