package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.StreamSequence;

import java.util.stream.Stream;

public class StreamFlow<T> implements Flow<T> {
    private final Stream<? extends T> stream;

    public StreamFlow(Stream<? extends T> stream) {
        this.stream = stream;
    }

    @Override
    public Sequence<T> open() {
        return new StreamSequence<>(stream);
    }
}
