package io.github.denyshorman.nanoflow;

/**
 * Factory class for creating {@link Flow} instances.
 * <p>
 * This class provides factory methods for creating flows with different concurrency
 * characteristics. All factory methods return cold flows that execute their emission
 * logic only when {@link Flow#collect(Collector)} is called.
 *
 * <h2>Available Flow Types</h2>
 * <ul>
 *   <li>{@link #flow(FlowAction)} - Sequential flow for single-threaded emission</li>
 *   <li>{@link #concurrentFlow(FlowAction)} - Concurrent flow for multithreaded emission</li>
 * </ul>
 *
 * @see Flow
 * @since 0.1.0
 */
public final class Flows {
    private Flows() {
    }

    /**
     * Creates a simple, sequential flow for single-threaded value emission.
     * <p>
     * The returned flow executes the emission logic on the thread that calls
     * {@link Flow#collect(Collector)}. The collector will be invoked sequentially
     * for each emitted value. This implementation has minimal overhead and is
     * suitable when all emissions happen from a single thread.
     *
     * <h2>Thread Safety</h2>
     * The emitter provided to the action should only be used from a single thread.
     * The collector will be called from the same thread that invokes {@code collect()}.
     *
     * <h2>Virtual Thread Friendly</h2>
     * This implementation works seamlessly with Java's Virtual Threads and does not
     * use any blocking synchronization primitives.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * var flow = Flows.<String>flow(emitter -> {
     *     emitter.emit("Hello");
     *     emitter.emit("World");
     * });
     *
     * var results = new ArrayList<String>();
     * flow.collect(results::add);
     * // results = ["Hello", "World"]
     * }</pre>
     *
     * @param action the emission logic that produces values; must not be null
     * @param <T>    the type of values emitted by the flow
     * @return a new sequential Flow that executes the given action
     * @throws NullPointerException if the action is null
     * @see #concurrentFlow(FlowAction)
     */
    public static <T> Flow<T> flow(FlowAction<T> action) {
        return new SimpleFlow<>(action);
    }

    /**
     * Creates a concurrent flow that supports multithreaded value emission with
     * serialized collector access.
     * <p>
     * The returned flow allows the emission logic to emit values from multiple threads
     * concurrently. While emissions can happen in parallel, the flow ensures that the
     * collector receives values in a serialized manner (one at a time), preventing
     * race conditions in the collector.
     *
     * <h3>Thread Safety</h3>
     * <ul>
     *   <li>The emitter is thread-safe and can be called from multiple threads concurrently</li>
     *   <li>The collector is guaranteed to receive values one at a time (serialized)</li>
     *   <li>Supports interruption via {@link Thread#interrupt()}</li>
     * </ul>
     *
     * <h3>Performance Considerations</h3>
     * Concurrent emission introduces synchronization overhead. Use this only when you
     * actually need to emit values from multiple threads. For single-threaded scenarios,
     * prefer {@link #flow(FlowAction)}.
     *
     * <h3>Virtual Thread Friendly</h3>
     * This implementation is designed to work efficiently with Java's Virtual Threads.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * var flow = Flows.<Integer>concurrentFlow(emitter -> {
     *     try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
     *         for (var i = 0; i < 1000; i++) {
     *             final var value = i;
     *             executor.submit(() -> emitter.emit(value));
     *         }
     *     } // waits for all tasks to complete
     * });
     *
     * var results = new ArrayList<Integer>();
     * flow.collect(results::add);
     * // results contains all 1000 values (order not guaranteed)
     * }</pre>
     *
     * <h3>Important Notes</h3>
     * <ul>
     *   <li>Collectors do not need to be thread-safe; the flow handles serialization</li>
     *   <li>Emission order is not guaranteed when multiple threads emit concurrently</li>
     * </ul>
     *
     * @param action the emission logic that produces values; must not be null
     * @param <T>    the type of values emitted by the flow
     * @return a new concurrent Flow that executes the given action
     * @throws NullPointerException if the action is null
     * @see #flow(FlowAction)
     */
    public static <T> Flow<T> concurrentFlow(FlowAction<T> action) {
        return new ConcurrentFlow<>(action);
    }
}
