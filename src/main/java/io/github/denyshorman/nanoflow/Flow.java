package io.github.denyshorman.nanoflow;

/**
 * A Flow represents a cold stream of values that are emitted when {@link #collect(Collector)} is called.
 * <p>
 * Unlike hot streams, a Flow does not emit values until a collector is attached. Each call to
 * {@code collect()} executes the flow's emission logic from the beginning.
 * <p>
 * Flows are designed to work seamlessly with Java's Virtual Threads and can support both
 * sequential and concurrent emission patterns depending on the implementation.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * var flow = Flows.<String>flow(emitter -> {
 *     emitter.emit("Hello");
 *     emitter.emit("World");
 * });
 *
 * flow.collect(value -> System.out.println("Received: " + value));
 * }</pre>
 *
 * @param <T> the type of values emitted by the flow
 * @see Flows#flow(FlowAction)
 * @see Flows#concurrentFlow(FlowAction)
 * @since 0.1.0
 */
public interface Flow<T> {
    /**
     * Starts the flow and collects emitted values using the provided collector.
     * <p>
     * This is a terminal, blocking operation that executes the flow's emission logic
     * and forwards all emitted values to the collector. The method returns only after
     * all values have been emitted or an exception occurs.
     * <p>
     * <b>Thread Safety:</b> The thread-safety guarantees of this method depend on the
     * specific Flow implementation. See {@link Flows#flow(FlowAction)} and
     * {@link Flows#concurrentFlow(FlowAction)} for details.
     * <p>
     * <b>Exception Handling:</b> Any exceptions thrown during emission, including checked
     * exceptions from {@link FlowAction}, will be propagated to the caller without wrapping.
     * If the collector throws an exception, the behavior is implementation-specific.
     * For concurrent flows, if the thread is interrupted, an {@code InterruptedException}
     * may be thrown as an unchecked exception.
     *
     * @param collector the collector that will receive emitted values; must not be null
     * @throws NullPointerException if collector is null
     * @see Collector
     */
    void collect(Collector<T> collector);
}
