package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;
import io.github.denyshorman.nanoflow.internal.util.UpstreamPump;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class TimeoutFlow<T> extends BufferedFlow<T> {
    public TimeoutFlow(Flow<? extends T> upstream, Duration timeout) {
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
                while (true) {
                    var element = queue.poll(timeoutNanos, TimeUnit.NANOSECONDS);

                    if (element == null) {
                        pump.interrupt();
                        throw new TimeoutException("Flow timed out after " + timeout);
                    }

                    if (element == stop) {
                        var throwable = error.get();

                        if (throwable != null) {
                            throw throwable;
                        }

                        return;
                    }

                    @SuppressWarnings("unchecked")
                    var value = (T) element;
                    emitter.emit(value);
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
