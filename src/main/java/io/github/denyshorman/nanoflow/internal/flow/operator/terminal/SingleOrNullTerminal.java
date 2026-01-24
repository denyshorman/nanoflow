package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

public class SingleOrNullTerminal<T> {
    private final Flow<? extends T> upstream;

    public SingleOrNullTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public @Nullable T evaluate() {
        try (var items = upstream.open()) {
            var iterator = items.iterator();

            if (!iterator.hasNext()) {
                return null;
            }

            var value = iterator.next();

            if (iterator.hasNext()) {
                throw new IllegalStateException("Flow has more than one element");
            }

            return value;
        }
    }
}
