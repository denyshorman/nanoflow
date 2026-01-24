package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.operator.MapSequence;

import java.util.function.Function;

public class MapFlow<T, R> implements Flow<R> {
    private final Flow<? extends T> upstream;
    private final Function<? super T, ? extends R> mapper;

    public MapFlow(
            Flow<? extends T> upstream,
            Function<? super T, ? extends R> mapper
    ) {
        this.upstream = upstream;
        this.mapper = mapper;
    }

    @Override
    public Sequence<R> open() {
        return new MapSequence<>(upstream.open(), mapper);
    }
}
