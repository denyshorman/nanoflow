package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

public class LastOrDefaultTerminal<T> {
    private final Flow<? extends T> upstream;
    private final T defaultValue;

    public LastOrDefaultTerminal(Flow<? extends T> upstream, T defaultValue) {
        this.upstream = upstream;
        this.defaultValue = defaultValue;
    }

    public T evaluate() {
        try (var items = upstream.open()) {
            T last = null;

            for (var item : items) {
                last = item;
            }

            return last == null ? defaultValue : last;
        }
    }
}
