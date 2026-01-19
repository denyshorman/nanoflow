package io.github.denyshorman.nanoflow;

/**
 * A functional interface representing the emission logic of a {@link Flow}.
 * <p>
 * This interface is used to define how values are produced and emitted into a flow.
 * Unlike standard functional interfaces, {@code FlowAction} is allowed to throw
 * checked exceptions, which will be propagated to the caller of
 * {@link Flow#collect(Collector)} without wrapping.
 *
 * <h2>Exception Handling</h2>
 * Any exception (checked or unchecked) thrown by the {@code perform} method will
 * be propagated directly to the caller of {@code collect()} without being wrapped
 * in a {@link RuntimeException}.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * FlowAction<String> action = emitter -> {
 *     var data = readFromFile(); // may throw IOException
 *     emitter.emit(data);
 * };
 *
 * var flow = Flows.<String>flow(action);
 * try {
 *     flow.collect(System.out::println);
 * } catch (IOException e) {
 *     // IOException propagated directly from readFromFile()
 * }
 * }</pre>
 *
 * @param <T> the type of values emitted by the flow
 * @see Flows#flow(FlowAction)
 * @see Flows#concurrentFlow(FlowAction)
 * @since 0.1.0
 */
@FunctionalInterface
public interface FlowAction<T> {
    /**
     * Performs the emission logic, pushing values into the flow via the provided emitter.
     * <p>
     * This method is called when {@link Flow#collect(Collector)} is invoked. The implementation
     * should use the emitter to push values that will be forwarded to the collector.
     *
     * @param emitter the emitter used to push values into the flow; never null
     * @throws Exception if an error occurs during emission; the exception will be
     *                   propagated to the caller of {@code collect()} without wrapping
     */
    void perform(Emitter<T> emitter) throws Exception;
}
