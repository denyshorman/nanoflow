package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.PublisherSequence;

import java.util.concurrent.Flow.Publisher;

public class PublisherFlow<T> implements Flow<T> {
    private final Publisher<? extends T> publisher;

    public PublisherFlow(Publisher<? extends T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public Sequence<T> open() {
        return new PublisherSequence<>(publisher);
    }
}
