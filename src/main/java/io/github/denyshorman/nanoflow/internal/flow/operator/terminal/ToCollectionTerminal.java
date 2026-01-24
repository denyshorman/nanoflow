package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

import java.util.Collection;
import java.util.function.Supplier;

public class ToCollectionTerminal<T, C extends Collection<T>> {
    private final Flow<? extends T> upstream;
    private final Supplier<C> supplier;

    public ToCollectionTerminal(Flow<? extends T> upstream, Supplier<C> supplier) {
        this.upstream = upstream;
        this.supplier = supplier;
    }

    public C evaluate() {
        var collection = supplier.get();
        try (var items = upstream.open()) {
            for (var item : items) {
                collection.add(item);
            }
        }
        return collection;
    }
}
