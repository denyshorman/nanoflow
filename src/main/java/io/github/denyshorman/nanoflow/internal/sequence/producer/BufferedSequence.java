package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class BufferedSequence<T> implements Flow.Sequence<T> {
    private static final Object STOP = new Object();

    private final Thread producerThread;
    private final BlockingQueue<Object> queue;
    private final AtomicReference<@Nullable Throwable> error = new AtomicReference<>();
    private volatile boolean closed = false;

    public BufferedSequence(Flow.Action<T> action) {
        this(0, action);
    }

    public BufferedSequence(int bufferSize, Flow.Action<T> action) {
        if (bufferSize < 0) {
            throw new IllegalArgumentException("buffer size must be non-negative or Integer.MAX_VALUE");
        } else if (bufferSize == 0) {
            queue = new SynchronousQueue<>();
        } else if (bufferSize == Integer.MAX_VALUE) {
            queue = new LinkedTransferQueue<>();
        } else {
            queue = new ArrayBlockingQueue<>(bufferSize);
        }

        producerThread = Thread.ofVirtual().start(() -> {
            try {
                action.perform(new EnqueueAction());
            } catch (Throwable t) {
                if (!closed) {
                    error.set(t);
                }
            } finally {
                if (!closed) {
                    try {
                        queue.put(STOP);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        });
    }

    @Override
    public Iterator<T> iterator() {
        if (closed) {
            throw new IllegalStateException("Sequence is closed");
        }

        return new Iterator<>() {
            private @Nullable Object nextElement = null;

            @Override
            public boolean hasNext() {
                if (nextElement != null) return true;
                if (closed) return false;

                Object element;

                try {
                    element = queue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw sneakyThrow(e);
                }

                if (element == STOP) {
                    closed = true;
                    var throwable = error.get();
                    if (throwable != null) {
                        if (throwable instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw sneakyThrow(throwable);
                    }
                    return false;
                }

                nextElement = element;

                return true;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (nextElement == null) throw new NoSuchElementException();
                var element = nextElement;
                nextElement = null;
                return (T) element;
            }
        };
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            producerThread.interrupt();
        }
    }

    private class EnqueueAction implements Flow.Emitter<T> {
        @Override
        public void emit(T value) throws InterruptedException {
            if (closed) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("Flow cancelled by downstream");
            }

            queue.put(value);
        }
    }
}
