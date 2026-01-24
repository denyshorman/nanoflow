package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.BufferedSequence;

public class BufferedFlow<T> implements Flow<T> {
    private final int bufferSize;
    private final Action<T> action;

    public BufferedFlow(Flow.Action<T> action) {
        this(0, action);
    }

    public BufferedFlow(int bufferSize, Flow.Action<T> action) {
        this.bufferSize = bufferSize;
        this.action = action;
    }

    @Override
    public Sequence<T> open() {
        return new BufferedSequence<>(bufferSize, action);
    }
}
