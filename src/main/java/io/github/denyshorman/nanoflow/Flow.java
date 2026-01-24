package io.github.denyshorman.nanoflow;

import io.github.denyshorman.nanoflow.internal.flow.operator.intermediate.*;
import io.github.denyshorman.nanoflow.internal.flow.operator.terminal.*;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * An ordered sequence of values.
 *
 * <h2>The open() + for-each Model</h2>
 * <p>The defining feature of Nanoflow is its manual iteration model. Instead of relying on callbacks,
 * a {@code Flow} is consumed by opening it and iterating using a standard Java for-each loop:
 *
 * <pre>{@code
 * try (var sequence = flow.open()) {
 *     for (var item : sequence) {
 *         System.out.println(item);
 *     }
 * }
 * }</pre>
 *
 * <p>With bounded buffers, the producer blocks when the consumer is not ready. The model works
 * seamlessly with Virtual Threads and allows checked exceptions to propagate naturally.
 *
 * <p>A {@code Flow} describes how values are produced when opened. The returned {@link Sequence}
 * must be closed to ensure that resources (like file systems, network connections, or thread pools)
 * are released and that producers are stopped.
 *
 * <p>Production begins when a flow is opened.
 *
 * <p>Flows do not allow {@code null} values. Emitting {@code null} or returning {@code null}
 * from a producer will result in a {@link NullPointerException}.
 *
 * <p>Exceptions from sources or intermediate operators are thrown during iteration or collection.
 *
 * @param <T> the type of values emitted by this flow
 * @see Flows
 */
public interface Flow<T> {
    /**
     * Opens a new sequence for iteration.
     *
     * <p>This method starts the production of values defined by this flow. The caller is responsible
     * for closing the returned sequence, ideally using a try-with-resources block.
     *
     * <p>The returned {@link Sequence} is single-consumer. Iteration and {@link Sequence#close()}
     * must happen on the same thread.
     *
     * <p>Closing the sequence signals cancellation to the producer via interruption. Producers should
     * handle interruption or periodically check {@link Thread#isInterrupted()} to stop.
     *
     * @return a new {@link Sequence} for this flow
     */
    Sequence<T> open();

    //#region Operators

    //#region Intermediate

    //#region Transforming

    /**
     * Returns a flow that applies {@code mapper} to each value.
     *
     * <p>Each element from the source flow is passed through the mapper function
     * to produce a new value of type {@code R}.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.of(1, 2, 3)
     *     .map(n -> n * 2)
     *     .toList(); // [2, 4, 6]
     * }</pre>
     *
     * @param mapper mapping function
     * @param <R>    mapped value type
     * @return a mapped flow
     */
    default <R> Flow<R> map(Function<? super T, ? extends R> mapper) {
        return new MapFlow<>(this, mapper);
    }

    /**
     * Returns a flow that maps each value to a flow and emits its values in order.
     *
     * <p>This operator is sequential: it opens the next inner flow only after the previous
     * one has completed. It does not merge inner flows concurrently.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.of(1, 2, 3)
     *     .flatMap(n -> Flows.of(n, n * 10))
     *     .toList(); // [1, 10, 2, 20, 3, 30]
     * }</pre>
     *
     * @param mapper mapping function that returns an inner flow
     * @param <R>    inner value type
     * @return a flattened flow
     */
    default <R> Flow<R> flatMap(Function<? super T, ? extends Flow<? extends R>> mapper) {
        return new FlatMapFlow<>(this, mapper);
    }

    /**
     * Returns a flow that emits {@code initial} and then running reductions.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.of(1, 2, 3)
     *     .scan(0, Integer::sum)
     *     .toList(); // [0, 1, 3, 6]
     * }</pre>
     *
     * @param initial initial accumulator value
     * @param reducer accumulator function
     * @param <R>     accumulator type
     * @return a flow of accumulated values
     */
    default <R> Flow<R> scan(R initial, BiFunction<? super R, ? super T, ? extends R> reducer) {
        return new ScanFlow<>(this, initial, reducer);
    }

    /**
     * Returns a flow that casts each value to {@code type}.
     *
     * <p>Example:
     * <pre>{@code
     * var flow = Flows.<Object>of("a", "b");
     * flow.cast(String.class).toList(); // ["a", "b"]
     * }</pre>
     *
     * @param type target class to cast to
     * @param <R>  target type
     * @return a flow with elements cast to {@code type}
     * @throws ClassCastException if a value cannot be cast to {@code type}
     */
    default <R> Flow<R> cast(Class<R> type) {
        return new CastFlow<>(this, type);
    }

    //#endregion

    //#region Filtering & Slicing

    /**
     * Returns a flow that emits values matching {@code predicate}.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.range(1, 10)
     *     .filter(n -> n % 2 == 0)
     *     .toList(); // [2, 4, 6, 8]
     * }</pre>
     *
     * @param predicate filter predicate
     * @return a filtered flow
     * @see #filterNot(Predicate)
     */
    default Flow<T> filter(Predicate<? super T> predicate) {
        return new FilterFlow<>(this, predicate, true);
    }

    /**
     * Returns a flow that emits values not matching {@code predicate}.
     *
     * @param predicate filter predicate
     * @return a filtered flow
     * @see #filter(Predicate)
     */
    default Flow<T> filterNot(Predicate<? super T> predicate) {
        return new FilterFlow<>(this, predicate, false);
    }

    /**
     * Returns a flow that emits at most {@code count} values.
     *
     * @param count maximum number of values to emit. Must be non-negative.
     * @return a truncated flow
     * @throws IllegalArgumentException if {@code count} is negative
     */
    default Flow<T> take(long count) {
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");
        if (count == 0) return Flows.empty();
        return new TakeFlow<>(this, count);
    }

    /**
     * Returns a flow that emits values while {@code predicate} is true.
     *
     * @param predicate predicate applied to each value
     * @return a truncated flow
     */
    default Flow<T> takeWhile(Predicate<? super T> predicate) {
        return new TakeWhileFlow<>(this, predicate);
    }

    /**
     * Returns a flow that skips the first {@code count} values.
     *
     * @param count number of values to skip. Must be non-negative.
     * @return a flow without the leading values
     * @throws IllegalArgumentException if {@code count} is negative
     */
    default Flow<T> drop(long count) {
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");
        return new DropFlow<>(this, count);
    }

    /**
     * Returns a flow that drops values while {@code predicate} is true, then emits the rest.
     *
     * @param predicate predicate applied to leading values
     * @return a flow without the leading values
     */
    default Flow<T> dropWhile(Predicate<? super T> predicate) {
        return new DropWhileFlow<>(this, predicate);
    }

    //#endregion

    //#region Distinct

    /**
     * Returns a flow that emits the first occurrence of each value.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.of(1, 2, 2, 3, 1)
     *     .distinct()
     *     .toList(); // [1, 2, 3]
     * }</pre>
     *
     * @return a flow without duplicates
     */
    default Flow<T> distinct() {
        return new DistinctFlow<>(this);
    }

    /**
     * Returns a flow that drops adjacent duplicates using {@link Objects#equals(Object, Object)}.
     *
     * @return a flow without adjacent duplicates
     */
    default Flow<T> distinctUntilChanged() {
        return new DistinctUntilChangedFlow<>(this);
    }

    //#endregion

    //#region Buffering & Batching

    /**
     * Returns a flow that buffers values with the given buffer size.
     *
     * <p>A buffer allows the producer to continue emitting values even if the consumer is slower.
     * When the buffer is full, the producer will block until the consumer makes more space.
     *
     * @param bufferSize the number of elements that can be stored in the buffer before the producer
     *                   blocks.
     *                   <ul>
     *                   <li><b>Positive value:</b> Creates a buffer of the specified capacity.</li>
     *                   <li><b>0:</b> No buffer is created; the producer and consumer synchronize
     *                   on each element (synchronous handoff).</li>
     *                   <li><b>{@link Integer#MAX_VALUE}:</b> Creates an effectively unbounded buffer
     *                   (limited only by available memory).</li>
     *                   </ul>
     * @return a buffered flow
     * @throws IllegalArgumentException if {@code bufferSize} is negative
     */
    default Flow<T> buffer(int bufferSize) {
        return new BufferFlow<>(this, bufferSize);
    }

    /**
     * Returns a flow that groups values into lists of up to {@code size}.
     *
     * <p>The last chunk may contain fewer elements if the flow does not divide evenly.
     * The emitted lists are immutable snapshots.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.range(1, 8)
     *     .chunked(3)
     *     .toList(); // [[1, 2, 3], [4, 5, 6], [7]]
     * }</pre>
     *
     * @param size chunk size. Must be positive.
     * @return a flow of chunks
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    default Flow<List<T>> chunked(int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be positive");
        return new ChunkedFlow<>(this, size);
    }

    /**
     * Returns a flow that groups values into lists produced at most once per {@code window}.
     *
     * <p>The timer starts when the first element of a batch arrives. Batches are not overlapping,
     * and empty batches are not emitted. The final batch (if any) is emitted when the upstream completes.
     *
     * @param window maximum time to wait before emitting a batch. Must be positive.
     * @return a flow of time-based batches
     * @throws IllegalArgumentException if {@code window} is zero or negative
     */
    default Flow<List<T>> chunked(Duration window) {
        if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("window must be > 0");
        return new ChunkedTimedFlow<>(this, window);
    }

    /**
     * Returns a flow that groups values into lists of up to {@code size}, emitting a batch
     * when {@code size} is reached or {@code maxWait} elapses since the first element in the batch.
     *
     * <p>Batches are not overlapping, and empty batches are not emitted. The final batch (if any)
     * is emitted when the upstream completes.
     *
     * @param size    maximum number of elements per batch. Must be positive.
     * @param maxWait maximum time to wait before emitting a batch. Must be positive.
     * @return a flow of size-or-time-based batches
     * @throws IllegalArgumentException if {@code size} is not positive or {@code maxWait} is zero or negative
     */
    default Flow<List<T>> chunked(int size, Duration maxWait) {
        if (size <= 0) throw new IllegalArgumentException("size must be positive");
        if (maxWait.isZero() || maxWait.isNegative()) throw new IllegalArgumentException("maxWait must be > 0");
        return new ChunkedTimedFlow<>(this, size, maxWait);
    }

    /**
     * Returns a flow that emits sliding windows of the given size and step.
     *
     * <p>Windows are non-overlapping when {@code step == size}, and overlapping when
     * {@code step < size}. Items that do not fall into any window (when {@code step > size})
     * are dropped. Partial windows are not emitted.
     *
     * @param size window size. Must be positive.
     * @param step number of elements between window starts. Must be positive.
     * @return a flow of windows
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    default Flow<List<T>> windowed(int size, int step) {
        if (size <= 0) throw new IllegalArgumentException("size must be positive");
        if (step <= 0) throw new IllegalArgumentException("size must be positive");
        return new WindowedSizeFlow<>(this, size, step);
    }

    /**
     * Returns a flow that emits sliding time windows.
     *
     * <p>Windows start at the time of the first element and then every {@code step}. Each window
     * spans {@code window} duration. Items are placed into all windows whose time ranges include them.
     * Partial windows are emitted on completion. Empty windows are not emitted.
     *
     * @param window window duration. Must be positive.
     * @param step   time between window starts. Must be positive.
     * @return a flow of windows
     * @throws IllegalArgumentException if {@code window} or {@code step} is zero or negative
     */
    default Flow<List<T>> windowed(Duration window, Duration step) {
        if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("window must be > 0");
        if (step.isZero() || step.isNegative()) throw new IllegalArgumentException("step must be > 0");
        return new WindowedTimedFlow<>(this, window, step);
    }

    //#endregion

    //#region Timing

    /**
     * Returns a flow that delays the start of collection by {@code delay}.
     *
     * @param delay delay before collection starts. Must be non-negative.
     * @return a delayed flow
     * @throws IllegalArgumentException if {@code delay} is negative
     */
    default Flow<T> delay(Duration delay) {
        if (delay.isNegative()) throw new IllegalArgumentException("delay must be >= 0");
        return new DelayFlow<>(this, delay);
    }

    /**
     * Returns a flow that delays each element by {@code delay}.
     *
     * @param delay delay before each element. Must be non-negative.
     * @return a delayed flow
     * @throws IllegalArgumentException if {@code delay} is negative
     */
    default Flow<T> delayEach(Duration delay) {
        if (delay.isNegative()) throw new IllegalArgumentException("delay must be >= 0");
        return new DelayEachFlow<>(this, delay);
    }

    /**
     * Returns a flow that emits the latest value only after the flow has been idle for {@code timeout}.
     *
     * <p>Each new value resets the timer. When the upstream completes, the latest value (if any)
     * is emitted immediately.
     *
     * @param timeout idle time required before emitting. Must be positive.
     * @return a debounced flow
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     */
    default Flow<T> debounce(Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be > 0");
        return new DebounceFlow<>(this, timeout);
    }

    /**
     * Returns a flow that emits the latest value at most once per {@code period}.
     *
     * <p>Each period emits the most recent value arrived since the last emission.
     * If no value arrived during a period, nothing is emitted. When the upstream completes,
     * the latest pending value (if any) is emitted immediately.
     *
     * @param period sampling period. Must be positive.
     * @return a sampled flow
     * @throws IllegalArgumentException if {@code period} is zero or negative
     */
    default Flow<T> sample(Duration period) {
        if (period.isZero() || period.isNegative()) throw new IllegalArgumentException("period must be > 0");
        return new SampleFlow<>(this, period);
    }

    /**
     * Returns a flow that fails if the next element is not received within {@code timeout}.
     *
     * <p>Timeout applies only while waiting for the next element. Time spent processing
     * downstream (for example, blocked in {@link Emitter#emit(Object)}) does not count.
     *
     * @param timeout maximum time to wait for the next element. Must be positive.
     * @return a timed flow
     * <p>A timeout results in {@link TimeoutException} being thrown during iteration or collection.
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     */
    default Flow<T> timeout(Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be > 0");
        return new TimeoutFlow<>(this, timeout);
    }
    //#endregion

    //#region Combining

    /**
     * Returns a flow that emits this flow and then {@code other}.
     *
     * @param other flow to append
     * @return a concatenated flow
     */
    default Flow<T> concat(Flow<? extends T> other) {
        return new ConcatFlow<>(this, other);
    }

    /**
     * Returns a flow that zips values from this flow and {@code other}.
     *
     * <p>The resulting flow ends when either flow ends.
     *
     * @param other  other flow
     * @param zipper function that combines values
     * @param <U>    other value type
     * @param <R>    result value type
     * @return a zipped flow
     */
    default <U, R> Flow<R> zip(
            Flow<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> zipper
    ) {
        return new ZipFlow<>(this, other, zipper);
    }

    //#endregion

    //#endregion

    //#region Terminal

    //#region Element Access

    /**
     * Returns the first element.
     *
     * @return the first element
     * @throws NoSuchElementException if the flow is empty
     */
    default T first() {
        return new FirstTerminal<>(this).evaluate();
    }

    /**
     * Returns the first element or {@code null} if the flow is empty.
     *
     * @return the first element, or {@code null} if empty
     */
    default @Nullable T firstOrNull() {
        return new FirstOrNullTerminal<>(this).evaluate();
    }

    /**
     * Returns the first element or {@code defaultValue} if the flow is empty.
     *
     * @param defaultValue value to return when the flow is empty
     * @return the first element or the default value
     */
    default T firstOrDefault(T defaultValue) {
        return new FirstOrDefaultTerminal<>(this, defaultValue).evaluate();
    }

    /**
     * Returns the last element.
     *
     * @return the last element
     * @throws NoSuchElementException if the flow is empty
     */
    default T last() {
        return new LastTerminal<>(this).evaluate();
    }

    /**
     * Returns the last element or {@code null} if the flow is empty.
     *
     * @return the last element or {@code null}
     */
    default @Nullable T lastOrNull() {
        return new LastOrNullTerminal<>(this).evaluate();
    }

    /**
     * Returns the last element or {@code defaultValue} if the flow is empty.
     *
     * @param defaultValue value to return when the flow is empty
     * @return the last element or the default value
     */
    default T lastOrDefault(T defaultValue) {
        return new LastOrDefaultTerminal<>(this, defaultValue).evaluate();
    }

    /**
     * Returns the only element.
     *
     * @return the single element
     * @throws NoSuchElementException if the flow is empty
     * @throws IllegalStateException  if the flow has more than one element
     */
    default T single() {
        return new SingleTerminal<>(this).evaluate();
    }

    /**
     * Returns the only element, or {@code null} if the flow is empty.
     *
     * @return the single element, or {@code null} if empty
     * @throws IllegalStateException if the flow has more than one element
     */
    default @Nullable T singleOrNull() {
        return new SingleOrNullTerminal<>(this).evaluate();
    }

    /**
     * Returns the only element, or {@code defaultValue} if the flow is empty.
     *
     * @param defaultValue value to return when the flow is empty
     * @return the single element or the default value
     * @throws IllegalStateException if the flow has more than one element
     */
    default T singleOrDefault(T defaultValue) {
        return new SingleOrDefaultTerminal<>(this, defaultValue).evaluate();
    }

    //#endregion

    //#region Matching

    /**
     * Returns {@code true} if any element matches {@code predicate}.
     *
     * <p>Returns {@code false} if the flow is empty.
     *
     * @param predicate predicate to test
     * @return {@code true} if any element matches
     */
    default boolean any(Predicate<? super T> predicate) {
        return new AnyTerminal<>(this, predicate).evaluate();
    }

    /**
     * Returns {@code true} if all elements match {@code predicate}.
     *
     * <p>Returns {@code true} if the flow is empty.
     *
     * @param predicate predicate to test
     * @return {@code true} if all elements match
     */
    default boolean all(Predicate<? super T> predicate) {
        return new AllTerminal<>(this, predicate).evaluate();
    }

    /**
     * Returns {@code true} if no elements match {@code predicate}.
     *
     * <p>Returns {@code true} if the flow is empty.
     *
     * @param predicate predicate to test
     * @return {@code true} if no elements match
     */
    default boolean none(Predicate<? super T> predicate) {
        return new NoneTerminal<>(this, predicate).evaluate();
    }

    //#endregion

    //#region Aggregation

    /**
     * Returns the total number of elements in this flow.
     *
     * @return element count
     */
    default long count() {
        return new CountTerminal<>(this).evaluate();
    }

    /**
     * Reduces the elements of this flow using the provided {@code reducer}.
     *
     * <p>Reduction starts with the first element of the flow.
     *
     * <p>Example:
     * <pre>{@code
     * var sum = Flows.of(1, 2, 3, 4)
     *     .reduce(Integer::sum); // 10
     * }</pre>
     *
     * @param reducer reduction function. Must not be null.
     * @return the reduced value
     * @throws NoSuchElementException if the flow is empty
     */
    default T reduce(BinaryOperator<T> reducer) {
        return new ReduceTerminal<>(this, reducer).evaluate();
    }

    /**
     * Folds the elements of this flow into {@code initial} using {@code accumulator}.
     *
     * <p>Returns {@code initial} if the flow is empty.
     *
     * <p>Example:
     * <pre>{@code
     * var product = Flows.of(1, 2, 3, 4)
     *     .fold(1, (a, b) -> a * b); // 24
     * }</pre>
     *
     * @param initial     initial accumulator value
     * @param accumulator function that combines accumulator with each element. Must not be null.
     * @param <R>         result type
     * @return the accumulated value
     */
    default <R> R fold(R initial, BiFunction<? super R, ? super T, ? extends R> accumulator) {
        return new FoldTerminal<>(this, initial, accumulator).evaluate();
    }

    //#endregion

    //#region Collection

    /**
     * Collects all elements into a list.
     *
     * @return list of all elements
     */
    default List<T> toList() {
        return new ToListTerminal<>(this).evaluate();
    }

    /**
     * Collects all elements into a linked hash set.
     *
     * @return set of all elements
     */
    default Set<T> toSet() {
        return new ToSetTerminal<>(this).evaluate();
    }

    /**
     * Collects all elements into a collection created by {@code supplier}.
     *
     * @param supplier collection factory
     * @param <C>      collection type
     * @return the populated collection
     */
    default <C extends Collection<T>> C toCollection(Supplier<C> supplier) {
        return new ToCollectionTerminal<>(this, supplier).evaluate();
    }

    //#endregion

    //#region Consumption

    /**
     * Consumes all elements with {@code collector}.
     *
     * <p>This is a terminal operator that handles the opening and closing of the flow automatically.
     * Checked exceptions thrown by the producer or the collector are propagated directly.
     *
     * <pre>{@code
     * flow.collect(value -> System.out.println(value));
     * }</pre>
     *
     * @param collector consumer of each element. Must not be null.
     * @param <E>       exception type thrown by the collector or producer
     * @throws E if the collector or producer throws an exception
     */
    default <E extends Exception> void collect(Collector<? super T, E> collector) throws E {
        new CollectTerminal<>(this, collector).run();
    }

    //#endregion

    //#endregion

    //#endregion

    //#region Interop

    /**
     * Returns a sequential {@link Stream} over this flow.
     *
     * <p>The returned stream must be closed to release resources and stop the flow's producers.
     * It is recommended to use the stream within a try-with-resources block.
     *
     * @return a sequential stream
     */
    default Stream<T> stream() {
        return new StreamTerminal<>(this).evaluate();
    }

    //#endregion

    //#region Interfaces

    /**
     * An iterable that must be closed to release resources.
     */
    interface Sequence<T> extends Iterable<T>, AutoCloseable {
        /**
         * Closes the sequence and releases resources.
         *
         * <p>This signals cancellation to the producer via interruption.
         * <p>Single-consumer: iteration and {@code close()} must be performed on the same thread.
         * <p>To cancel from another thread, interrupt the thread performing the iteration.
         */
        @Override
        void close();
    }

    /**
     * Producer callback used by {@link Flows#flow(Action)}.
     */
    @FunctionalInterface
    interface Action<T> {
        /**
         * Produces values by calling {@link Emitter#emit(Object)}.
         *
         * @param emitter value emitter
         * @throws Exception if production fails
         */
        void perform(Emitter<T> emitter) throws Exception;
    }

    /**
     * Emits values to a downstream consumer.
     *
     * <p>Implementations of {@link Action#perform(Emitter)} use this to send values to the flow.
     * Emitters are thread-safe: multiple threads can call {@link #emit(Object)} concurrently.
     * Values will be queued and delivered to the consumer sequentially in the order they were accepted.
     */
    @FunctionalInterface
    interface Emitter<T> {
        /**
         * Emits a value to the downstream consumer.
         *
         * <p>This method blocks if the internal buffer is full (backpressure). If the flow is
         * canceled or the emitting thread is interrupted, this method throws
         * {@link InterruptedException}.
         *
         * @param value value to emit. Must not be null.
         * @throws InterruptedException if the flow is canceled or the emitting thread is interrupted
         * @throws NullPointerException if {@code value} is null
         */
        void emit(T value) throws InterruptedException;
    }

    /**
     * Consumes values with a checked exception type.
     */
    @FunctionalInterface
    interface Collector<T, E extends Exception> {
        /**
         * Accepts a value.
         *
         * @param value value to consume
         * @throws E if processing fails
         */
        void accept(T value) throws E;
    }

    //#endregion
}
