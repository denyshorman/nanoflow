package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.operator.CastSequence;

public class CastFlow<R> implements Flow<R> {
    private final Flow<?> upstream;
    private final Class<R> type;

    public CastFlow(Flow<?> upstream, Class<R> type) {
        this.upstream = upstream;
        this.type = type;
    }

    @Override
    public Sequence<R> open() {
        return new CastSequence<>(upstream.open(), type);
    }
}
