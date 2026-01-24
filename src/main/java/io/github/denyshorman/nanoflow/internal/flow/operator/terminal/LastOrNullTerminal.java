package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

public class LastOrNullTerminal<T> {
    private final Flow<? extends T> upstream;

    public LastOrNullTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public @Nullable T evaluate() {
        try (var items = upstream.open()) {
            T last = null;

            for (var item : items) {
                last = item;
            }

            return last;
        }
    }
}
