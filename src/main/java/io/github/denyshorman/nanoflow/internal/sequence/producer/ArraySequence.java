package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Iterator;

public class ArraySequence<T> implements Flow.Sequence<T> {
    private final T[] items;

    public ArraySequence(T[] items) {
        this.items = items;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < items.length;
            }

            @Override
            public T next() {
                return items[index++];
            }
        };
    }

    @Override
    public void close() {
    }
}
