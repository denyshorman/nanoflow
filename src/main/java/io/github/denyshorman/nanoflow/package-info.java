/**
 * Core Flow API for ordered sequences with explicit lifecycle control.
 *
 * <h2>Overview</h2>
 * <p>Nanoflow is a minimal library for reactive programming focused on simplicity.
 * It provides a {@link io.github.denyshorman.nanoflow.Flow} interface which represents a sequence
 * of values that can be produced concurrently and consumed sequentially.
 *
 * <h2>The open() + for-each Model</h2>
 * <p>A central design choice of Nanoflow is its reliance on standard Java constructs for flow consumption.
 * Instead of complex subscription callbacks or chainable operators, a flow is typically consumed
 * by opening a {@link io.github.denyshorman.nanoflow.Flow.Sequence} and iterating over it using
 * a standard for-each loop.
 *
 * <p>With bounded buffers, the producer blocks if the consumer is not ready,
 * ensures resource safety through the {@link java.lang.AutoCloseable} nature of the sequence,
 * and allows checked exceptions to propagate naturally.
 *
 * <pre>{@code
 * try (var items = flow.open()) {
 *     for (var item : items) {
 *         // Handle item
 *     }
 * }
 * }</pre>
 *
 * <h2>Key Features</h2>
 * <ul>
 *     <li><b>Concurrent Emission:</b> Multiple threads can emit values into the same flow concurrently,
 *     and values are observed in arrival order.</li>
 *     <li><b>Resource Management:</b> Flows are {@link io.github.denyshorman.nanoflow.Flow.Sequence} based,
 *     requiring explicit closing to release resources.</li>
 *     <li><b>Virtual Thread Friendly:</b> Designed to work seamlessly with Java 21+ virtual threads.</li>
 * </ul>
 */
package io.github.denyshorman.nanoflow;
