package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Iterator;

public class IterableSequence<T> implements Flow.Sequence<T> {
    private final Iterable<? extends T> iterable;

    public IterableSequence(Iterable<? extends T> iterable) {
        this.iterable = iterable;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        return (Iterator<T>) iterable.iterator();
    }

    @Override
    public void close() {
    }
}
