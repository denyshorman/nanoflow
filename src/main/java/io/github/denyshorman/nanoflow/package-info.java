/**
 * Provides a lightweight, concurrent flow implementation for Java 21+.
 * <p>
 * Nanoflow offers a simple API for creating cold streams that support both sequential
 * and concurrent value emission patterns. The library is designed to work seamlessly
 * with Java's Virtual Threads while providing minimal overhead.
 *
 * <h2>Core Concepts</h2>
 * <dl>
 *   <dt><b>Flow</b></dt>
 *   <dd>A cold stream that emits values only when {@link io.github.denyshorman.nanoflow.Flow#collect(io.github.denyshorman.nanoflow.Collector) collect()}
 *       is called. Each collection starts the emission process from the beginning.</dd>
 *
 *   <dt><b>Emitter</b></dt>
 *   <dd>Used within the flow action to push values into the flow.</dd>
 *
 *   <dt><b>Collector</b></dt>
 *   <dd>Receives and processes values emitted by the flow.</dd>
 *
 *   <dt><b>Sequential Flow</b></dt>
 *   <dd>Created via {@link io.github.denyshorman.nanoflow.Flows#flow(io.github.denyshorman.nanoflow.FlowAction)}.
 *       Designed for single-threaded emission with no synchronization overhead.</dd>
 *
 *   <dt><b>Concurrent Flow</b></dt>
 *   <dd>Created via {@link io.github.denyshorman.nanoflow.Flows#concurrentFlow(io.github.denyshorman.nanoflow.FlowAction)}.
 *       Supports multi-threaded emission while ensuring the collector receives values serially.</dd>
 * </dl>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * import io.github.denyshorman.nanoflow.Flows;
 * import java.util.ArrayList;
 * import java.util.List;
 *
 * // Create a sequential flow
 * var flow = Flows.<String>flow(emitter -> {
 *     emitter.emit("Hello");
 *     emitter.emit("World");
 * });
 *
 * // Collect values
 * var results = new ArrayList<String>();
 * flow.collect(results::add);
 * System.out.println(results); // [Hello, World]
 * }</pre>
 *
 * <h2>Concurrent Emission Example</h2>
 * <pre>{@code
 * import io.github.denyshorman.nanoflow.Flows;
 * import java.util.concurrent.Executors;
 *
 * var flow = Flows.<Integer>concurrentFlow(emitter -> {
 *     try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
 *         for (var i = 0; i < 100; i++) {
 *             final var value = i;
 *             executor.submit(() -> emitter.emit(value));
 *         }
 *     } // Executor waits for all tasks to complete
 * });
 *
 * flow.collect(value -> System.out.println("Received: " + value));
 * }</pre>
 *
 * <h2>Exception Handling</h2>
 * Checked exceptions thrown in flow actions are propagated to the caller without wrapping:
 * <pre>{@code
 * var flow = Flows.<String>flow(emitter -> {
 *     var data = readFile(); // may throw IOException
 *     emitter.emit(data);
 * });
 *
 * try {
 *     flow.collect(System.out::println);
 * } catch (IOException e) {
 *     // IOException is thrown directly, not wrapped
 *     System.err.println("Failed to read file: " + e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li><b>Sequential flows:</b> No thread safety guarantees. Use from a single thread.</li>
 *   <li><b>Concurrent flows:</b> Emitter is thread-safe. Collector receives values serially.</li>
 * </ul>
 *
 * @see io.github.denyshorman.nanoflow.Flows
 * @see io.github.denyshorman.nanoflow.Flow
 * @since 0.1.0
 */
package io.github.denyshorman.nanoflow;
