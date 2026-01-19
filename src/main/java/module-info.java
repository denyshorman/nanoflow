/**
 * Nanoflow - A lightweight, concurrent flow implementation for Java 21+.
 * <p>
 * This module provides a simple API for creating cold streams (flows) that support
 * both sequential and concurrent value emission. Nanoflow is designed to work
 * seamlessly with Java's Virtual Threads and provides minimal overhead for
 * single-threaded flows while ensuring a thread-safe collection for concurrent flows.
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link io.github.denyshorman.nanoflow.Flow} - The core flow interface</li>
 *   <li>{@link io.github.denyshorman.nanoflow.Flows} - Factory methods for creating flows</li>
 *   <li>{@link io.github.denyshorman.nanoflow.Emitter} - Interface for emitting values</li>
 *   <li>{@link io.github.denyshorman.nanoflow.Collector} - Interface for collecting values</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Sequential flow
 * var flow = Flows.<String>flow(emitter -> {
 *     emitter.emit("Hello");
 *     emitter.emit("World");
 * });
 * flow.collect(System.out::println);
 *
 * // Concurrent flow
 * var concurrentFlow = Flows.<Integer>concurrentFlow(emitter -> {
 *     try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
 *         for (var i = 0; i < 1000; i++) {
 *             final var value = i;
 *             executor.submit(() -> emitter.emit(value));
 *         }
 *     }
 * });
 * concurrentFlow.collect(System.out::println);
 * }</pre>
 *
 * @since 0.1.0
 */
module io.github.denyshorman.nanoflow {
    exports io.github.denyshorman.nanoflow;
}
