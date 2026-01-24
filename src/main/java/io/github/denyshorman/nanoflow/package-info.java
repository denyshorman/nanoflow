/**
 * Public API for creating and consuming flows.
 *
 * <h2>Creating Flows</h2>
 * Use {@link io.github.denyshorman.nanoflow.Flows} to create flows from sources and generators:
 *
 * <pre>{@code
 * var fromQueue = Flows.from(queue);
 * var range = Flows.range(1, 10);
 * var ticks = Flows.interval(Duration.ofSeconds(1));
 * }</pre>
 *
 * <h2>Custom Producers</h2>
 * For custom production, use {@link io.github.denyshorman.nanoflow.Flows#flow(io.github.denyshorman.nanoflow.Flow.Action)}.
 * The {@link io.github.denyshorman.nanoflow.Flow.Action} receives a thread-safe
 * {@link io.github.denyshorman.nanoflow.Flow.Emitter} that supports concurrent emission.
 *
 * <pre>{@code
 * var flow = Flows.flow(emitter -> {
 *     emitter.emit("a");
 *     emitter.emit("b");
 * });
 * }</pre>
 *
 * <h2>Custom Operators</h2>
 * Implement operators by returning a new {@link io.github.denyshorman.nanoflow.Flow} that wraps an
 * upstream flow. Use {@link io.github.denyshorman.nanoflow.Flow#open()} to consume upstream values
 * and emit transformed values.
 *
 * <pre>{@code
 * static <T, R> Flow<R> map(Flow<T> upstream, Function<? super T, ? extends R> mapper) {
 *     return Flows.flow(emitter -> {
 *         try (var items = upstream.open()) {
 *             for (var item : items) {
 *                 emitter.emit(mapper.apply(item));
 *             }
 *         }
 *     });
 * }
 * }</pre>
 *
 * <h2>Sequence Contract</h2>
 * A {@link io.github.denyshorman.nanoflow.Flow.Sequence} is single-consumer and must be closed to
 * cancel production and release resources. Iteration and {@code close()} should happen on the same
 * thread.
 */
package io.github.denyshorman.nanoflow;
