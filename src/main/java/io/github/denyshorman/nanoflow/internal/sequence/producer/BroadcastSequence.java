package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class BroadcastSequence<T> implements Flow.Sequence<T> {
    private final int bufferSize;
    private final Set<BroadcastSequence<T>> subscribers;
    private final ArrayDeque<T> queue = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private boolean sequenceClosed;
    private boolean broadcastClosed;

    public BroadcastSequence(int bufferSize, Set<BroadcastSequence<T>> subscribers) {
        this.bufferSize = bufferSize;
        this.subscribers = subscribers;

        subscribers.add(this);
    }

    public void awaitSpace() throws InterruptedException {
        lock.lockInterruptibly();

        try {
            var capacity = Math.max(1, bufferSize);

            while (!sequenceClosed && !broadcastClosed && queue.size() >= capacity) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public void offer(T value) {
        lock.lock();

        try {
            if (!sequenceClosed && !broadcastClosed) {
                queue.addLast(value);
                notEmpty.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public void awaitConsumed() throws InterruptedException {
        lock.lockInterruptibly();

        try {
            while (!sequenceClosed && !broadcastClosed && !queue.isEmpty()) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public void complete() {
        lock.lock();

        try {
            broadcastClosed = true;
            subscribers.remove(this);
            notEmpty.signalAll();
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        lock.lock();

        try {
            if (sequenceClosed) {
                throw new IllegalStateException("Sequence is closed");
            }
        } finally {
            lock.unlock();
        }

        return new Iterator<>() {
            private @Nullable T nextElement;

            @Override
            public boolean hasNext() {
                if (nextElement != null) {
                    return true;
                }

                lock.lock();

                try {
                    while (!sequenceClosed) {
                        var item = queue.pollFirst();

                        if (item != null) {
                            nextElement = item;
                            notFull.signalAll();
                            return true;
                        }

                        if (broadcastClosed) {
                            return false;
                        }

                        try {
                            notEmpty.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw sneakyThrow(e);
                        }
                    }

                    return false;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public T next() {
                if (nextElement == null) {
                    throw new NoSuchElementException("No more elements");
                }

                var element = nextElement;
                nextElement = null;

                return element;
            }
        };
    }

    @Override
    public void close() {
        lock.lock();

        try {
            if (!sequenceClosed) {
                sequenceClosed = true;
                subscribers.remove(this);
                queue.clear();
                notEmpty.signalAll();
                notFull.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
