package io.github.denyshorman.nanoflow.internal.sequence.operator;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Iterator;
import java.util.function.Function;

public class MapSequence<T, R> implements Flow.Sequence<R> {
    private final Flow.Sequence<? extends T> upstream;
    private final Function<? super T, ? extends R> mapper;

    public MapSequence(Flow.Sequence<? extends T> upstream, Function<? super T, ? extends R> mapper) {
        this.upstream = upstream;
        this.mapper = mapper;
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
                return mapper.apply(iterator.next());
            }
        };
    }

    @Override
    public void close() {
        upstream.close();
    }
}
