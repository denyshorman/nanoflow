package io.github.denyshorman.nanoflow;

/**
 * A functional interface for collecting values emitted by a {@link Flow}.
 * <p>
 * The collector is called for each value emitted by the flow during the execution
 * of {@link Flow#collect(Collector)}. Implementations should be side-effecting operations
 * such as adding to a collection, printing to console, or performing I/O.
 *
 * <h2>Thread Safety</h2>
 * When used with {@link Flows#flow(FlowAction)}, the collector will be called sequentially
 * from a single thread and does not need to be thread-safe.
 * <p>
 * When used with {@link Flows#concurrentFlow(FlowAction)}, even though multiple threads may
 * emit values concurrently, the collector is guaranteed to be called in a serialized manner
 * (one value at a time). This means the collector itself does not need to be thread-safe,
 * as only one thread will call {@code collect()} at any given time. However, if the collector
 * modifies a shared state accessed by other threads outside the collection process,
 * that shared state must still be properly synchronized.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * var results = new ArrayList<String>();
 * flow.collect(results::add);
 * }</pre>
 *
 * @param <T> the type of values to collect
 * @see Flow#collect(Collector)
 * @since 0.1.0
 */
@FunctionalInterface
public interface Collector<T> {
    /**
     * Accepts and processes a value emitted by the flow.
     * <p>
     * This method is called once for each emitted value. Implementations should
     * process the value and return quickly to avoid blocking the flow emission.
     *
     * @param value the emitted value; may be null if the flow emits null values
     * @throws Exception if an error occurs during the collection
     */
    void accept(T value) throws Exception;
}
