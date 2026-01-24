package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;
import io.github.denyshorman.nanoflow.internal.util.UpstreamPump;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class DebounceFlow<T> extends BufferedFlow<T> {
    public DebounceFlow(Flow<? extends T> upstream, Duration timeout) {
        super(new Action<>(upstream, timeout));
    }

    private record Action<T>(Flow<? extends T> upstream, Duration timeout) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) {
            final var timeoutNanos = timeout.toNanos();

            final var pump = new UpstreamPump<T>(upstream);
            final var queue = pump.queue();
            final var stop = pump.stop();
            final var error = pump.error();

            try {
                var pending = (Object) null;
                var deadlineNanos = 0L;

                while (true) {
                    Object element;

                    if (pending == null) {
                        element = queue.take();
                    } else {
                        var remaining = deadlineNanos - System.nanoTime();

                        if (remaining <= 0L) {
                            @SuppressWarnings("unchecked")
                            var value = (T) pending;
                            emitter.emit(value);
                            pending = null;
                            continue;
                        }

                        element = queue.poll(remaining, TimeUnit.NANOSECONDS);

                        if (element == null) {
                            @SuppressWarnings("unchecked")
                            var value = (T) pending;
                            emitter.emit(value);
                            pending = null;
                            continue;
                        }
                    }

                    if (element == stop) {
                        if (pending != null) {
                            @SuppressWarnings("unchecked")
                            var value = (T) pending;
                            emitter.emit(value);
                        }

                        var throwable = error.get();

                        if (throwable != null) {
                            throw throwable;
                        }

                        return;
                    }

                    pending = element;
                    deadlineNanos = System.nanoTime() + timeoutNanos;
                }
            } catch (Throwable e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                throw sneakyThrow(e);
            } finally {
                pump.interrupt();
            }
        }
    }
}
