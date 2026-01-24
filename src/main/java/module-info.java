import org.jspecify.annotations.NullMarked;

/**
 * Nanoflow is a Java 21+ Flow library with imperative iteration, virtual‑thread‑native backpressure,
 * cooperative cancellation, and transparent checked exceptions.
 *
 * <h2>Motivation</h2>
 * Modern applications often deal with asynchronous data: messages from queues, bytes from sockets,
 * or work completed on background threads. This asynchronicity means that producers can outrun
 * consumers, and failure or cancellation need to stop the source cleanly. The challenge lies not
 * in mapping values but in controlling the flow of data.
 *
 * <p>Traditionally, Reactive Streams offered a solution, but at the cost of complexity. They pushed
 * developers into a world of callbacks, schedulers, and intricate stack traces. Although effective,
 * this approach often resulted in code that was hard to read and maintain.
 *
 * <p>The landscape changed with Java 21's introduction of virtual threads. Blocking operations
 * became cheap and efficient again. This breakthrough allows developers to return to a simpler
 * model: produce data on one thread, consume it imperatively, and let backpressure act as a real
 * blocking signal.
 *
 * <p>Nanoflow is designed for this modern paradigm. It enables you to open a flow, iterate over it
 * using a familiar {@code for} loop, and close it to cancel. Checked exceptions remain visible,
 * backpressure is automatic, and the code retains the ordinary Java feel while handling asynchronous
 * producers seamlessly.
 *
 * <h2>Core Principles</h2>
 * <ul>
 *     <li><b>Imperative by Design:</b> Open a flow and iterate with {@code for-each}. The
 *     {@link io.github.denyshorman.nanoflow.Flow#open()} model brings the simplicity of
 *     {@link java.lang.Iterable} to asynchronous streams.</li>
 *     <li><b>Virtual Thread Native:</b> Blocking in producers or consumers is efficient on
 *     virtual threads, eliminating complex non-blocking state machines.</li>
 *     <li><b>Concurrent Emission:</b> Multiple threads can emit into a single flow; the consumer
 *     sees one ordered sequence.</li>
 *     <li><b>Null Safety:</b> Embraces JSpecify to make nullability explicit and improve tooling checks.</li>
 *     <li><b>Minimal Dependencies:</b> Only the JDK and JSpecify annotations.</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * var flow = Flows.<String>flow(emitter -> {
 *     emitter.emit("Hello");
 *     emitter.emit("World");
 * });
 *
 * try (var items = flow.open()) {
 *     for (var item : items) {
 *         System.out.println(item);
 *     }
 * }
 * }</pre>
 *
 * @see io.github.denyshorman.nanoflow.Flow
 * @see io.github.denyshorman.nanoflow.Flows
 */
@NullMarked
module io.github.denyshorman.nanoflow {
    requires transitive org.jspecify;
    exports io.github.denyshorman.nanoflow;
}
