package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamTerminal<T> {
    private final Flow<T> upstream;

    public StreamTerminal(Flow<T> upstream) {
        this.upstream = upstream;
    }

    public Stream<T> evaluate() {
        var sequence = upstream.open();
        var characteristics = Spliterator.ORDERED | Spliterator.NONNULL;

        var stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(sequence.iterator(), characteristics),
                false
        );

        return stream.onClose(sequence::close);
    }
}
