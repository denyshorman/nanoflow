package io.github.denyshorman.nanoflow.internal.sequence;

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

/**
 * A {@link Flow.Sequence} backed by a producer thread and a blocking queue.
 *
 * <p>The producer runs the supplied action and emits values into the queue.
 * The iterator consumes values and propagates any producer error.
 */
public class BufferedSequence<T> implements Flow.Sequence<T> {
    private static final Object STOP = new Object();

    private final Thread producerThread;
    private final BlockingQueue<Object> queue;
    private final AtomicReference<@Nullable Throwable> error = new AtomicReference<>();
    private volatile boolean closed = false;

    /**
     * Creates a sequence that buffers values produced by {@code action}.
     *
     * @param bufferSize buffer size (0 for synchronous handoff, Integer.MAX_VALUE for unbounded)
     * @param action     producer action
     * @throws IllegalArgumentException if {@code bufferSize} is negative
     */
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

        producerThread = Thread.ofVirtual().name("flow-producer-").start(() -> {
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

    /**
     * Returns an iterator that blocks on the internal queue.
     *
     * @return an iterator for this sequence
     * @throws IllegalStateException if the sequence is closed
     */
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

    /**
     * Closes the sequence and interrupts the producer thread.
     */
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
