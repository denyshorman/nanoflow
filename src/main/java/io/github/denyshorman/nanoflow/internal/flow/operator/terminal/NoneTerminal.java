package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.function.Predicate;

public class NoneTerminal<T> {
    private final Flow<? extends T> upstream;
    private final Predicate<? super T> predicate;

    public NoneTerminal(Flow<? extends T> upstream, Predicate<? super T> predicate) {
        this.upstream = upstream;
        this.predicate = predicate;
    }

    public boolean evaluate() {
        try (var items = upstream.open()) {
            for (var item : items) {
                if (predicate.test(item)) {
                    return false;
                }
            }
        }
        return true;
    }
}
