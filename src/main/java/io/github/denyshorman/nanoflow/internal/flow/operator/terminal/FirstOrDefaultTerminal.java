package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

public class FirstOrDefaultTerminal<T> {
    private final Flow<? extends T> upstream;
    private final T defaultValue;

    public FirstOrDefaultTerminal(Flow<? extends T> upstream, T defaultValue) {
        this.upstream = upstream;
        this.defaultValue = defaultValue;
    }

    public T evaluate() {
        try (var items = upstream.open()) {
            for (var item : items) {
                return item;
            }
        }

        return defaultValue;
    }
}
