package io.github.denyshorman.nanoflow.internal.flow.operator.terminal;

import io.github.denyshorman.nanoflow.Flow;

public class CollectTerminal<T, E extends Exception> {
    private final Flow<? extends T> upstream;
    private final Flow.Collector<? super T, E> collector;

    public CollectTerminal(Flow<? extends T> upstream, Flow.Collector<? super T, E> collector) {
        this.upstream = upstream;
        this.collector = collector;
    }

    public void run() throws E {
        try (var items = upstream.open()) {
            for (var item : items) {
                collector.accept(item);
            }
        }
    }
}
