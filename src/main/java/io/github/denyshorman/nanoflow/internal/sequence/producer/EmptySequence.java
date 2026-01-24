package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Iterator;

import static java.util.Collections.emptyIterator;

public class EmptySequence<T> implements Flow.Sequence<T> {
    private static final EmptySequence<?> INSTANCE = new EmptySequence<>();

    private EmptySequence() {
    }

    @SuppressWarnings("unchecked")
    public static <T> EmptySequence<T> instance() {
        return (EmptySequence<T>) INSTANCE;
    }

    @Override
    public Iterator<T> iterator() {
        return emptyIterator();
    }

    @Override
    public void close() {
    }
}
