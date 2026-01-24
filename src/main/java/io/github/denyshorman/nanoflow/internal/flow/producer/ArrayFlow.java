package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.ArraySequence;

public class ArrayFlow<T> implements Flow<T> {
    private final T[] items;

    public ArrayFlow(T[] items) {
        this.items = items;
    }

    @Override
    public Sequence<T> open() {
        return new ArraySequence<>(items);
    }
}
