package io.github.denyshorman.nanoflow;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.denyshorman.nanoflow.SneakyThrow.sneakyThrow;

/**
 * A concurrent implementation of {@link Flow} that supports multithreaded emission
 * while ensuring serialized access to the collector.
 * <p>
 * This implementation allows multiple threads to emit values concurrently.
 *
 * <h2>Exception Handling Philosophy</h2>
 * When a collector throws an exception, it propagates back through {@link Emitter#emit(Object)}
 * to only the emitter thread that triggered it. Other emitter threads continue independently.
 * This behavior is analogous to "await all" semantics in structured concurrency, where all
 * tasks run to completion even if some fail, allowing for partial processing and graceful degradation.
 * <p>
 * For fail-fast behavior where any failure should stop all emitter threads (analogous to
 * "all successful or throw" semantics), clients should use structured concurrency patterns
 * with appropriate scope management to coordinate cancellation across all emitter threads.
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *   <li>Uses {@link ReentrantLock#lockInterruptibly()} for synchronization</li>
 *   <li>Supports thread interruption during emission</li>
 *   <li>Uses non-fair locking for better performance (threads may acquire lock out of order)</li>
 *   <li>Virtual thread friendly (lock is released properly, minimizing pinning)</li>
 *   <li>Exceptions from the action are propagated without wrapping</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * Each call to {@code emit()} acquires a lock, which introduces contention when
 * many threads emit concurrently. For high-throughput scenarios, consider batching
 * emissions or using a single-threaded flow with a queue.
 *
 * @param <T> the type of values emitted by the flow
 * @see Flows#concurrentFlow(FlowAction)
 */
class ConcurrentFlow<T> implements Flow<T> {
    private final FlowAction<T> action;
    private final Lock lock = new ReentrantLock();

    ConcurrentFlow(FlowAction<T> action) {
        this.action = action;
    }

    @Override
    public void collect(Collector<T> collector) {
        try {
            action.perform(value -> {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw sneakyThrow(e);
                }

                try {
                    collector.accept(value);
                } catch (Exception e) {
                    throw sneakyThrow(e);
                } finally {
                    lock.unlock();
                }
            });
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }
}
