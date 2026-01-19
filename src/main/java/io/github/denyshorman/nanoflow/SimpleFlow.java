package io.github.denyshorman.nanoflow;

import static io.github.denyshorman.nanoflow.SneakyThrow.sneakyThrow;

/**
 * A simple, sequential implementation of {@link Flow} for single-threaded emission.
 * <p>
 * This implementation executes the flow action on the calling thread and directly
 * forwards emitted values to the collector without any synchronization overhead.
 * It is designed for scenarios where all emissions happen from a single thread.
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *   <li>No synchronization or locking - minimal overhead</li>
 *   <li>Direct pass-through of emitted values to the collector</li>
 *   <li>Exceptions from the action are propagated without wrapping</li>
 *   <li>Virtual thread friendly (no blocking synchronization)</li>
 * </ul>
 *
 * @param <T> the type of values emitted by the flow
 * @see Flows#flow(FlowAction)
 */
class SimpleFlow<T> implements Flow<T> {
    private final FlowAction<T> action;

    SimpleFlow(FlowAction<T> action) {
        this.action = action;
    }

    @Override
    public void collect(Collector<T> collector) {
        try {
            action.perform((Emitter<T>) value -> {
                try {
                    collector.accept(value);
                } catch (Exception e) {
                    throw sneakyThrow(e);
                }
            });
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }
}
