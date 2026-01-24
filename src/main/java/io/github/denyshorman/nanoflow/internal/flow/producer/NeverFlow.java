package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

public class NeverFlow<T> extends BufferedFlow<T> {
    public NeverFlow() {
        super(new Action<>());
    }

    private static class Action<T> implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws Exception {
            Thread.sleep(Long.MAX_VALUE);
        }
    }
}
