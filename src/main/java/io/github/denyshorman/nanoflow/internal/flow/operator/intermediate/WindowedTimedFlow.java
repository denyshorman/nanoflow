package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;
import io.github.denyshorman.nanoflow.internal.util.UpstreamPump;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class WindowedTimedFlow<T> extends BufferedFlow<List<T>> {
    public WindowedTimedFlow(Flow<? extends T> upstream, Duration window, Duration step) {
        super(new Action<>(upstream, window, step));
    }

    private record Action<T>(
            Flow<? extends T> upstream,
            Duration window,
            Duration step
    ) implements Flow.Action<List<T>> {
        public void perform(Emitter<List<T>> emitter) {
            final var windowNanos = window.toNanos();
            final var stepNanos = step.toNanos();

            final var pump = new UpstreamPump<T>(upstream, item -> new TimedValue<>(item, System.nanoTime()));
            final var queue = pump.queue();
            final var stop = pump.stop();
            final var error = pump.error();

            try {
                var windows = new ArrayDeque<Window<T>>();
                var started = false;
                var nextWindowStart = 0L;

                while (true) {
                    var now = System.nanoTime();

                    while (!windows.isEmpty() && windows.peekFirst().endNanos <= now) {
                        var finished = windows.removeFirst();

                        if (!finished.items().isEmpty()) {
                            emitter.emit(List.copyOf(finished.items()));
                        }
                    }

                    Object element;

                    if (!started) {
                        element = queue.take();
                    } else if (windows.isEmpty()) {
                        element = queue.take();
                    } else {
                        var waitNanos = Math.max(0L, windows.peekFirst().endNanos - now);

                        if (waitNanos == 0L) {
                            continue;
                        }

                        element = queue.poll(waitNanos, TimeUnit.NANOSECONDS);

                        if (element == null) {
                            continue;
                        }
                    }

                    if (element == stop) {
                        while (!windows.isEmpty()) {
                            var finished = windows.removeFirst();

                            if (!finished.items().isEmpty()) {
                                emitter.emit(List.copyOf(finished.items()));
                            }
                        }

                        var throwable = error.get();

                        if (throwable != null) {
                            throw throwable;
                        }

                        return;
                    }

                    @SuppressWarnings("unchecked")
                    var timed = (TimedValue<T>) element;
                    var timestamp = timed.timestampNanos();

                    if (!started) {
                        started = true;
                        nextWindowStart = timestamp;
                    }

                    while (!windows.isEmpty() && windows.peekFirst().endNanos <= timestamp) {
                        var finished = windows.removeFirst();

                        if (!finished.items().isEmpty()) {
                            emitter.emit(List.copyOf(finished.items()));
                        }
                    }

                    while (nextWindowStart <= timestamp) {
                        windows.addLast(new Window<>(nextWindowStart + windowNanos));
                        nextWindowStart += stepNanos;
                    }

                    for (var active : windows) {
                        if (timestamp < active.endNanos()) {
                            active.items().add(timed.value());
                        }
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

    private record TimedValue<T>(T value, long timestampNanos) {
    }

    private record Window<T>(long endNanos, ArrayList<T> items) {
        Window(long endNanos) {
            this(endNanos, new ArrayList<>());
        }
    }
}
