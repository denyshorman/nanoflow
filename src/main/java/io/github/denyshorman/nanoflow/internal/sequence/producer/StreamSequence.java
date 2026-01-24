package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Iterator;
import java.util.stream.Stream;

public class StreamSequence<T> implements Flow.Sequence<T> {
    private final Stream<? extends T> stream;

    public StreamSequence(Stream<? extends T> stream) {
        this.stream = stream;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        return (Iterator<T>) stream.iterator();
    }

    @Override
    public void close() {
        stream.close();
    }
}
