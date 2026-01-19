package io.github.denyshorman.nanoflow;

/**
 * A functional interface for emitting values into a {@link Flow}.
 * <p>
 * An emitter is provided to {@link FlowAction} implementations to push values
 * into the flow. The emitted values will be forwarded to the {@link Collector}
 * attached to the flow.
 *
 * <h2>Thread Safety</h2>
 * For flows created with {@link Flows#flow(FlowAction)}, the emitter should only
 * be called from a single thread (the thread executing the flow action).
 * <p>
 * For flows created with {@link Flows#concurrentFlow(FlowAction)}, the emitter
 * is thread-safe and can be called concurrently from multiple threads. The
 * concurrent flow implementation ensures that values are delivered to the
 * collector in a serialized manner.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Flows.<Integer>concurrentFlow(emitter -> {
 *     try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
 *         for (var i = 0; i < 100; i++) {
 *             final var value = i;
 *             executor.submit(() -> emitter.emit(value));
 *         }
 *     }
 * });
 * }</pre>
 *
 * @param <T> the type of values to emit
 * @see FlowAction
 * @since 0.1.0
 */
@FunctionalInterface
public interface Emitter<T> {
    /**
     * Emits a value into the flow.
     * <p>
     * For concurrent flows, this method blocks until the collector has finished
     * processing the previous value, ensuring serialized access to the collector.
     *
     * <h3>Exception Propagation</h3>
     * If the collector throws an exception while processing this value, the exception
     * will propagate back through this {@code emit()} call to the emitter thread.
     * In concurrent flows, this allows individual emitter threads to handle failures
     * independently - one emitter thread receiving an exception does not affect other
     * emitter threads.
     * <p>
     * This behavior is analogous to "await all" semantics in structured concurrency,
     * where all tasks run to completion even if some fail. Emitter threads can catch
     * and handle exceptions independently, allowing for graceful degradation or partial
     * processing.
     *
     * <h3>Fail-Fast with Structured Concurrency</h3>
     * For fail-fast behavior where any collector exception should stop all emitter threads
     * (analogous to "all successful or throw" semantics), clients should use structured
     * concurrency patterns with appropriate scope management.
     *
     * @param value the value to emit; may be null
     */
    void emit(T value);
}
