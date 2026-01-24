package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

public class FirstOrNullTerminal<T> {
    private final Flow<? extends T> upstream;

    public FirstOrNullTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public @Nullable T evaluate() {
        try (var items = upstream.open()) {
            for (var item : items) {
                return item;
            }
        }

        return null;
    }
}
