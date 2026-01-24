package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.IterableSequence;

public class IterableFlow<T> implements Flow<T> {
    private final Iterable<? extends T> items;

    public IterableFlow(Iterable<? extends T> items) {
        this.items = items;
    }

    @Override
    public Sequence<T> open() {
        return new IterableSequence<>(items);
    }
}
