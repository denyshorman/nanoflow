package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;
import io.github.denyshorman.nanoflow.internal.util.UpstreamPump;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class ChunkedTimedFlow<T> extends BufferedFlow<List<T>> {
    public ChunkedTimedFlow(Flow<? extends T> upstream, Duration window) {
        super(new Action<>(upstream, 0, window.toNanos()));
    }

    public ChunkedTimedFlow(Flow<? extends T> upstream, int size, Duration maxWait) {
        super(new Action<>(upstream, size, maxWait.toNanos()));
    }

    private record Action<T>(
            Flow<? extends T> upstream,
            int size,
            long maxWaitNanos
    ) implements Flow.Action<List<T>> {
        public void perform(Emitter<List<T>> emitter) {
            final var sizeBounded = size > 0;

            final var pump = new UpstreamPump<T>(upstream);
            final var queue = pump.queue();
            final var stop = pump.stop();
            final var error = pump.error();

            try {
                var batch = sizeBounded ? new ArrayList<T>(size) : new ArrayList<T>();
                var deadlineNanos = 0L;

                while (true) {
                    Object element;

                    if (batch.isEmpty()) {
                        element = queue.take();
                    } else {
                        var remaining = deadlineNanos - System.nanoTime();

                        if (remaining <= 0L) {
                            emitter.emit(List.copyOf(batch));
                            batch.clear();
                            continue;
                        }

                        element = queue.poll(remaining, TimeUnit.NANOSECONDS);

                        if (element == null) {
                            emitter.emit(List.copyOf(batch));
                            batch.clear();
                            continue;
                        }
                    }

                    if (element == stop) {
                        if (!batch.isEmpty()) {
                            emitter.emit(List.copyOf(batch));
                        }

                        var throwable = error.get();

                        if (throwable != null) {
                            throw throwable;
                        }

                        return;
                    }

                    @SuppressWarnings("unchecked")
                    var value = (T) element;

                    if (batch.isEmpty()) {
                        deadlineNanos = System.nanoTime() + maxWaitNanos;
                    }

                    batch.add(value);

                    if (sizeBounded && batch.size() == size) {
                        emitter.emit(List.copyOf(batch));
                        batch.clear();
                    }
                }
            } catch (Throwable t) {
                if (t instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                throw sneakyThrow(t);
            } finally {
                pump.interrupt();
            }
        }
    }
}
