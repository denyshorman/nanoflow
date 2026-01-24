package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.function.BiFunction;

public class FoldTerminal<T, R> {
    private final Flow<? extends T> upstream;
    private final R initial;
    private final BiFunction<? super R, ? super T, ? extends R> accumulator;

    public FoldTerminal(
            Flow<? extends T> upstream,
            R initial,
            BiFunction<? super R, ? super T, ? extends R> accumulator
    ) {
        this.upstream = upstream;
        this.initial = initial;
        this.accumulator = accumulator;
    }

    public R evaluate() {
        var result = initial;
        try (var items = upstream.open()) {
            for (var item : items) {
                result = accumulator.apply(result, item);
            }
        }
        return result;
    }
}
