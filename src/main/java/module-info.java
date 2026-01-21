import org.jspecify.annotations.NullMarked;

/**
 * Nanoflow is a Java 21+ Flow library with concurrent emission.
 *
 * <p>Nanoflow is designed around a simple imperative model. It allows flows to be opened and iterated using standard Java
 * {@code try-with-resources} and {@code for-each} loops. This model naturally supports checked
 * exceptions, allowing them to propagate directly from producers to consumers.
 *
 * <h2>Core Principles</h2>
 * <ul>
 *     <li><b>Imperative by Design:</b> Use standard Java control flow instead of specialized operators
 *     for common tasks. The {@link io.github.denyshorman.nanoflow.Flow#open()} model brings the simplicity
 *     of {@link java.lang.Iterable} to asynchronous streams.</li>
 *     <li><b>Virtual Thread Native:</b> Built for the Java 21+ era. Blocking in producers or consumers
 *     is handled gracefully by virtual threads, eliminating the need for complex non-blocking state machines.</li>
 *     <li><b>Concurrent Emission:</b> Native support for multiple threads emitting values into a single flow.
 *     The consumer receives a single sequence in arrival order.</li>
 *     <li><b>Null Safety:</b> Flows do not allow {@code null} values. The library uses JSpecify annotations
 *     to provide compile-time nullness checks.</li>
 *     <li><b>Minimal Dependencies:</b> A lightweight library that only requires the JDK and JSpecify annotations.</li>
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
