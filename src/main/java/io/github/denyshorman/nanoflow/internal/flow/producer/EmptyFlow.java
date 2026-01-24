package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.BufferedSequence;
import io.github.denyshorman.nanoflow.internal.sequence.producer.EmptySequence;

public class EmptyFlow<T> implements Flow<T> {
    private static final EmptyFlow<?> INSTANCE = new EmptyFlow<>();

    private EmptyFlow() {
    }

    @SuppressWarnings("unchecked")
    public static <T> EmptyFlow<T> instance() {
        return (EmptyFlow<T>) INSTANCE;
    }

    @Override
    public Sequence<T> open() {
        return EmptySequence.instance();
    }
}
