package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.ArrayList;
import java.util.List;

public class ToListTerminal<T> {
    private final Flow<? extends T> upstream;

    public ToListTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public List<T> evaluate() {
        var list = new ArrayList<T>();
        try (var items = upstream.open()) {
            for (var item : items) {
                list.add(item);
            }
        }
        return list;
    }
}
