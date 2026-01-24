package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.NoSuchElementException;

public class FirstTerminal<T> {
    private final Flow<? extends T> upstream;

    public FirstTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public T evaluate() {
        try (var items = upstream.open()) {
            for (var item : items) {
                return item;
            }
        }

        throw new NoSuchElementException("Flow is empty");
    }
}
