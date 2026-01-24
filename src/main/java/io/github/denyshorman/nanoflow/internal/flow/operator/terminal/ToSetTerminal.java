package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.LinkedHashSet;
import java.util.Set;

public class ToSetTerminal<T> {
    private final Flow<? extends T> upstream;

    public ToSetTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public Set<T> evaluate() {
        var set = new LinkedHashSet<T>();
        try (var items = upstream.open()) {
            for (var item : items) {
                set.add(item);
            }
        }
        return set;
    }
}
