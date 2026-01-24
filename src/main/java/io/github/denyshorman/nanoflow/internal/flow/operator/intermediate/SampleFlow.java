package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;
import io.github.denyshorman.nanoflow.internal.util.UpstreamPump;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class SampleFlow<T> extends BufferedFlow<T> {
    public SampleFlow(Flow<? extends T> upstream, Duration period) {
        super(new Action<>(upstream, period));
    }

    private record Action<T>(Flow<? extends T> upstream, Duration period) implements Flow.Action<T> {
        @Override
        public void perform(Emitter<T> emitter) {
            final var periodNanos = period.toNanos();

            final var pump = new UpstreamPump<T>(upstream);
            final var queue = pump.queue();
            final var stop = pump.stop();
            final var error = pump.error();

            try {
                var last = (Object) null;
                var hasNew = false;
                var nextTick = 0L;

                while (true) {
                    Object element;

                    if (last == null) {
                        element = queue.take();
                    } else {
                        var remaining = nextTick - System.nanoTime();

                        if (remaining <= 0L) {
                            if (hasNew) {
                                @SuppressWarnings("unchecked")
                                var value = (T) last;
                                emitter.emit(value);
                                hasNew = false;
                            }

                            nextTick = System.nanoTime() + periodNanos;
                            continue;
                        }

                        element = queue.poll(remaining, TimeUnit.NANOSECONDS);

                        if (element == null) {
                            if (hasNew) {
                                @SuppressWarnings("unchecked")
                                var value = (T) last;
                                emitter.emit(value);
                                hasNew = false;
                            }

                            nextTick = System.nanoTime() + periodNanos;
                            continue;
                        }
                    }

                    if (element == stop) {
                        if (hasNew) {
                            @SuppressWarnings("unchecked")
                            var value = (T) last;
                            emitter.emit(value);
                        }

                        var throwable = error.get();

                        if (throwable != null) {
                            throw throwable;
                        }

                        return;
                    }

                    last = element;
                    hasNew = true;

                    if (nextTick == 0L) {
                        nextTick = System.nanoTime() + periodNanos;
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
