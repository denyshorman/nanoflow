package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.NoSuchElementException;

public class SingleTerminal<T> {
    private final Flow<? extends T> upstream;

    public SingleTerminal(Flow<? extends T> upstream) {
        this.upstream = upstream;
    }

    public T evaluate() {
        try (var items = upstream.open()) {
            var iterator = items.iterator();

            if (!iterator.hasNext()) {
                throw new NoSuchElementException("Flow is empty");
            }

            var value = iterator.next();

            if (iterator.hasNext()) {
                throw new IllegalStateException("Flow has more than one element");
            }

            return value;
        }
    }
}
