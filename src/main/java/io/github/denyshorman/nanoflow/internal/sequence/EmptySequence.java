package io.github.denyshorman.nanoflow.internal.sequence;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Iterator;

import static java.util.Collections.emptyIterator;

/**
 * A {@link Flow.Sequence} with no elements.
 */
public final class EmptySequence<T> implements Flow.Sequence<T> {
    private static final EmptySequence<?> INSTANCE = new EmptySequence<>();

    private EmptySequence() {
    }

    /**
     * Returns a shared empty sequence instance.
     *
     * @param <T> element type
     * @return empty sequence
     */
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
