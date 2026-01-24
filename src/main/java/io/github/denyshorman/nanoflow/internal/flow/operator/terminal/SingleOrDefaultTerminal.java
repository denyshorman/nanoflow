package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

public class SingleOrDefaultTerminal<T> {
    private final Flow<? extends T> upstream;
    private final T defaultValue;

    public SingleOrDefaultTerminal(Flow<? extends T> upstream, T defaultValue) {
        this.upstream = upstream;
        this.defaultValue = defaultValue;
    }

    public T evaluate() {
        try (var items = upstream.open()) {
            var iterator = items.iterator();

            if (!iterator.hasNext()) {
                return defaultValue;
            }

            var value = iterator.next();

            if (iterator.hasNext()) {
                throw new IllegalStateException("Flow has more than one element");
            }

            return value;
        }
    }
}
