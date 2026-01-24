package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

public class CountTerminal<T> {
    private final Flow<? extends T> upstream;

    public CountTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public long evaluate() {
        var count = 0L;
        try (var items = upstream.open()) {
            for (var ignored : items) {
                count++;
            }
        }
        return count;
    }
}
