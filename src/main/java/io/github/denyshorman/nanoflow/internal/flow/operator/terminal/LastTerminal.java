package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.NoSuchElementException;

public class LastTerminal<T> {
    private final Flow<? extends T> upstream;

    public LastTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public T evaluate() {
        try (var items = upstream.open()) {
            T last = null;

            for (var item : items) {
                last = item;
            }

            if (last == null) {
                throw new NoSuchElementException("Flow is empty");
            }

            return last;
        }
    }
}
