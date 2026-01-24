package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.NoSuchElementException;
import java.util.function.BinaryOperator;

public class ReduceTerminal<T> {
    private final Flow<? extends T> upstream;
    private final BinaryOperator<T> reducer;

    public ReduceTerminal(Flow<? extends T> upstream, BinaryOperator<T> reducer) {
        this.upstream = upstream;
        this.reducer = reducer;
    }

    public T evaluate() {
        try (var items = upstream.open()) {
            var iter = items.iterator();

            if (!iter.hasNext()) {
                throw new NoSuchElementException("Flow is empty");
            }

            var value = iter.next();

            while (iter.hasNext()) {
                value = reducer.apply(value, iter.next());
            }

            return value;
        }
    }
}
