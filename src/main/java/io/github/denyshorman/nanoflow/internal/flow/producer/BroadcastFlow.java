package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.BroadcastSequence;
import io.github.denyshorman.nanoflow.internal.sequence.producer.EmptySequence;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class BroadcastFlow<T> implements Flow.Broadcast<T> {
    private final int bufferSize;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ReentrantLock emitLock = new ReentrantLock();
    private final Set<BroadcastSequence<T>> subscribers = ConcurrentHashMap.newKeySet();

    public BroadcastFlow(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public void emit(T value) throws InterruptedException {
        emitLock.lockInterruptibly();

        try {
            if (closed.get()) {
                throw new IllegalStateException("Broadcast is closed");
            }

            for (var subscriber : subscribers) {
                subscriber.awaitSpace();
            }

            for (var subscriber : subscribers) {
                subscriber.offer(value);
            }

            if (bufferSize == 0) {
                for (var subscriber : subscribers) {
                    subscriber.awaitConsumed();
                }
            }
        } finally {
            emitLock.unlock();
        }
    }

    @Override
    public Sequence<T> open() {
        if (closed.get()) {
            return EmptySequence.instance();
        }

        var sequence = new BroadcastSequence<>(bufferSize, subscribers);

        if (closed.get()) {
            sequence.complete();
        }

        return sequence;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (var subscriber : subscribers) {
                subscriber.complete();
            }
        }
    }
}
