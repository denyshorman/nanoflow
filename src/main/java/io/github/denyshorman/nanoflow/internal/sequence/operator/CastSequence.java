package io.github.denyshorman.nanoflow.internal.sequence.operator;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Iterator;

public class CastSequence<R> implements Flow.Sequence<R> {
    private final Flow.Sequence<?> upstream;
    private final Class<R> type;

    public CastSequence(Flow.Sequence<?> upstream, Class<R> type) {
        this.upstream = upstream;
        this.type = type;
    }

    @Override
    public Iterator<R> iterator() {
        var iterator = upstream.iterator();

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public R next() {
                return type.cast(iterator.next());
            }
        };
    }

    @Override
    public void close() {
        upstream.close();
    }
}
