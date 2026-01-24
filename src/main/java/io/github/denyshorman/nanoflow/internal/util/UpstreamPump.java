package io.github.denyshorman.nanoflow.internal.util;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class UpstreamPump<T> {
    private final Thread thread;
    private final Object stop = new Object();
    private final SynchronousQueue<Object> queue = new SynchronousQueue<>();
    private final AtomicReference<@Nullable Throwable> error = new AtomicReference<>();

    public UpstreamPump(Flow<? extends T> upstream) {
        this(upstream, Function.identity());
    }

    public UpstreamPump(Flow<? extends T> upstream, Function<? super T, ?> mapper) {
        thread = Thread.ofVirtual().start(() -> {
            try (var items = upstream.open()) {
                for (var item : items) {
                    queue.put(mapper.apply(item));
                }
            } catch (Throwable t) {
                error.set(t);
            } finally {
                try {
                    queue.put(stop);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    public Object stop() {
        return stop;
    }

    public SynchronousQueue<Object> queue() {
        return queue;
    }

    public AtomicReference<@Nullable Throwable> error() {
        return error;
    }

    public void interrupt() {
        thread.interrupt();
    }
}
