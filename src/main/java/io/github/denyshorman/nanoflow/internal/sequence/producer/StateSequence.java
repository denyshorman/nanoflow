package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class StateSequence<T> implements Flow.Sequence<T> {
    private final Supplier<T> valueSupplier;
    private final Set<StateSequence<T>> subscribers;
    private volatile @Nullable Thread waitingThread;
    private volatile boolean closed;

    public StateSequence(Supplier<T> valueSupplier, Set<StateSequence<T>> subscribers) {
        this.valueSupplier = valueSupplier;
        this.subscribers = subscribers;

        subscribers.add(this);
    }

    public void wakeUp() {
        LockSupport.unpark(waitingThread);
    }

    @Override
    public Iterator<T> iterator() {
        if (closed) {
            throw new IllegalStateException("Sequence is closed");
        }

        return new Iterator<>() {
            private @Nullable T nextElement;
            private @Nullable T lastEmitted;

            @Override
            public boolean hasNext() {
                if (nextElement != null) {
                    return true;
                }

                waitingThread = Thread.currentThread();

                try {
                    while (!closed) {
                        var current = valueSupplier.get();

                        if (!Objects.equals(current, lastEmitted)) {
                            nextElement = current;
                            return true;
                        }

                        LockSupport.park(StateSequence.this);

                        if (Thread.currentThread().isInterrupted()) {
                            throw sneakyThrow(new InterruptedException("Sequence iteration interrupted"));
                        }
                    }
                } finally {
                    waitingThread = null;
                }

                return false;
            }

            @Override
            public T next() {
                if (nextElement == null) {
                    throw new NoSuchElementException("No more elements");
                }

                var element = nextElement;

                nextElement = null;
                lastEmitted = element;

                return element;
            }
        };
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            subscribers.remove(this);
            LockSupport.unpark(waitingThread);
        }
    }
}
