package io.github.denyshorman.nanoflow;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.denyshorman.nanoflow.Flows.flow;
import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

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
        return flow(emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    emitter.emit(mapper.apply(item));
                }
            }
        });
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
        return flow(emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    var innerFlow = mapper.apply(item);

                    try (var innerUpstream = innerFlow.open()) {
                        for (var innerItem : innerUpstream) {
                            emitter.emit(innerItem);
                        }
                    }
                }
            }
        });
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
        return flow(emitter -> {
            var accumulator = initial;
            emitter.emit(accumulator);

            try (var upstream = open()) {
                for (var item : upstream) {
                    accumulator = reducer.apply(accumulator, item);
                    emitter.emit(accumulator);
                }
            }
        });
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
        return flow(emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    if (predicate.test(item)) {
                        emitter.emit(item);
                    }
                }
            }
        });
    }

    /**
     * Returns a flow that emits values not matching {@code predicate}.
     *
     * @param predicate filter predicate
     * @return a filtered flow
     * @see #filter(Predicate)
     */
    default Flow<T> filterNot(Predicate<? super T> predicate) {
        return flow(emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    if (!predicate.test(item)) {
                        emitter.emit(item);
                    }
                }
            }
        });
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

        return flow(emitter -> {
            if (count == 0) {
                return;
            }

            try (var upstream = open()) {
                var remaining = count;
                for (var item : upstream) {
                    emitter.emit(item);
                    if (--remaining == 0) {
                        break;
                    }
                }
            }
        });
    }

    /**
     * Returns a flow that emits values while {@code predicate} is true.
     *
     * @param predicate predicate applied to each value
     * @return a truncated flow
     */
    default Flow<T> takeWhile(Predicate<? super T> predicate) {
        return flow(emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    if (!predicate.test(item)) {
                        break;
                    }
                    emitter.emit(item);
                }
            }
        });
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

        return flow(emitter -> {
            try (var upstream = open()) {
                var remaining = count;
                for (var item : upstream) {
                    if (remaining > 0) {
                        remaining--;
                        continue;
                    }
                    emitter.emit(item);
                }
            }
        });
    }

    /**
     * Returns a flow that drops values while {@code predicate} is true, then emits the rest.
     *
     * @param predicate predicate applied to leading values
     * @return a flow without the leading values
     */
    default Flow<T> dropWhile(Predicate<? super T> predicate) {
        return flow(emitter -> {
            try (var upstream = open()) {
                var dropping = true;
                for (var item : upstream) {
                    if (dropping && predicate.test(item)) {
                        continue;
                    }
                    dropping = false;
                    emitter.emit(item);
                }
            }
        });
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
        return flow(emitter -> {
            var seen = new HashSet<T>();
            try (var upstream = open()) {
                for (var item : upstream) {
                    if (seen.add(item)) {
                        emitter.emit(item);
                    }
                }
            }
        });
    }

    /**
     * Returns a flow that drops adjacent duplicates using {@link Objects#equals(Object, Object)}.
     *
     * @return a flow without adjacent duplicates
     */
    default Flow<T> distinctUntilChanged() {
        return flow(emitter -> {
            var last = (Object) null;
            var hasLast = false;
            try (var upstream = open()) {
                for (var item : upstream) {
                    if (!hasLast || !Objects.equals(item, last)) {
                        emitter.emit(item);
                        last = item;
                        hasLast = true;
                    }
                }
            }
        });
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
        return flow(bufferSize, emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    emitter.emit(item);
                }
            }
        });
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

        return flow(emitter -> {
            try (var upstream = open()) {
                var chunk = new ArrayList<T>(size);

                for (var item : upstream) {
                    chunk.add(item);

                    if (chunk.size() == size) {
                        emitter.emit(List.copyOf(chunk));
                        chunk.clear();
                    }
                }

                if (!chunk.isEmpty()) {
                    emitter.emit(List.copyOf(chunk));
                }
            }
        });
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

        return flow(emitter -> {
            if (!delay.isZero()) {
                Thread.sleep(delay);
            }

            try (var upstream = open()) {
                for (var item : upstream) {
                    emitter.emit(item);
                }
            }
        });
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

        return flow(emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    if (!delay.isZero()) {
                        Thread.sleep(delay);
                    }
                    emitter.emit(item);
                }
            }
        });
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

        return flow(emitter -> {
            var stop = new Object();
            var timeoutNanos = timeout.toNanos();
            var queue = new SynchronousQueue<>();
            var error = new AtomicReference<@Nullable Throwable>();

            var upstreamThread = Thread.ofVirtual().name("flow-timeout-upstream-").start(() -> {
                try (var upstream = open()) {
                    for (var item : upstream) {
                        queue.put(item);
                    }
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    try {
                        queue.put(stop);
                    } catch (InterruptedException ignored) {
                    }
                }
            });

            try {
                while (true) {
                    Object element;

                    try {
                        element = queue.poll(timeoutNanos, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw sneakyThrow(e);
                    }

                    if (element == null) {
                        upstreamThread.interrupt();
                        throw new TimeoutException("Flow timed out after " + timeout);
                    }

                    if (element == stop) {
                        var throwable = error.get();
                        if (throwable != null) {
                            if (throwable instanceof InterruptedException) {
                                Thread.currentThread().interrupt();
                            }
                            throw sneakyThrow(throwable);
                        }
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    var value = (T) element;
                    emitter.emit(value);
                }
            } finally {
                upstreamThread.interrupt();
            }
        });
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
        return flow(emitter -> {
            try (var upstream = open()) {
                for (var item : upstream) {
                    emitter.emit(item);
                }
            }

            try (var downstream = other.open()) {
                for (var item : downstream) {
                    emitter.emit(item);
                }
            }
        });
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
        return flow(emitter -> {
            try (var left = open(); var right = other.open()) {
                var leftIter = left.iterator();
                var rightIter = right.iterator();
                while (leftIter.hasNext() && rightIter.hasNext()) {
                    var zipped = zipper.apply(leftIter.next(), rightIter.next());
                    emitter.emit(zipped);
                }
            }
        });
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
        try (var upstream = open()) {
            for (var item : upstream) {
                return item;
            }
        }

        throw new NoSuchElementException("Flow is empty");
    }

    /**
     * Returns the first element or {@code null} if the flow is empty.
     *
     * @return the first element, or {@code null} if empty
     */
    default @Nullable T firstOrNull() {
        try (var upstream = open()) {
            for (var item : upstream) {
                return item;
            }
        }

        return null;
    }

    /**
     * Returns the first element or {@code defaultValue} if the flow is empty.
     *
     * @param defaultValue value to return when the flow is empty
     * @return the first element or the default value
     */
    default T firstOrDefault(T defaultValue) {
        try (var upstream = open()) {
            for (var item : upstream) {
                return item;
            }
        }

        return defaultValue;
    }

    /**
     * Returns the last element.
     *
     * @return the last element
     * @throws NoSuchElementException if the flow is empty
     */
    default T last() {
        try (var upstream = open()) {
            T last = null;

            for (var item : upstream) {
                last = item;
            }

            if (last == null) {
                throw new NoSuchElementException("Flow is empty");
            }

            return last;
        }
    }

    /**
     * Returns the last element or {@code null} if the flow is empty.
     *
     * @return the last element or {@code null}
     */
    default @Nullable T lastOrNull() {
        try (var upstream = open()) {
            T last = null;

            for (var item : upstream) {
                last = item;
            }

            return last;
        }
    }

    /**
     * Returns the last element or {@code defaultValue} if the flow is empty.
     *
     * @param defaultValue value to return when the flow is empty
     * @return the last element or the default value
     */
    default T lastOrDefault(T defaultValue) {
        try (var upstream = open()) {
            T last = null;

            for (var item : upstream) {
                last = item;
            }

            return last == null ? defaultValue : last;
        }
    }

    /**
     * Returns the only element.
     *
     * @return the single element
     * @throws NoSuchElementException if the flow is empty
     * @throws IllegalStateException  if the flow has more than one element
     */
    default T single() {
        try (var upstream = open()) {
            var iterator = upstream.iterator();

            if (!iterator.hasNext()) {
                throw new NoSuchElementException("Flow is empty");
            }

            var value = iterator.next();

            if (iterator.hasNext()) {
                throw new IllegalStateException("Flow has more than one element");
            }

            return value;
        }
    }

    /**
     * Returns the only element, or {@code null} if the flow is empty.
     *
     * @return the single element, or {@code null} if empty
     * @throws IllegalStateException if the flow has more than one element
     */
    default @Nullable T singleOrNull() {
        try (var upstream = open()) {
            var iterator = upstream.iterator();

            if (!iterator.hasNext()) {
                return null;
            }

            var value = iterator.next();

            if (iterator.hasNext()) {
                throw new IllegalStateException("Flow has more than one element");
            }

            return value;
        }
    }

    /**
     * Returns the only element, or {@code defaultValue} if the flow is empty.
     *
     * @param defaultValue value to return when the flow is empty
     * @return the single element or the default value
     * @throws IllegalStateException if the flow has more than one element
     */
    default T singleOrDefault(T defaultValue) {
        try (var upstream = open()) {
            var iterator = upstream.iterator();

            if (!iterator.hasNext()) {
                return defaultValue;
            }

            var value = iterator.next();

            if (iterator.hasNext()) {
                throw new IllegalStateException("Flow has more than one element");
            }

            return value;
        }
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
        try (var upstream = open()) {
            for (var item : upstream) {
                if (predicate.test(item)) {
                    return true;
                }
            }
        }
        return false;
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
        try (var upstream = open()) {
            for (var item : upstream) {
                if (!predicate.test(item)) {
                    return false;
                }
            }
        }
        return true;
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
        try (var upstream = open()) {
            for (var item : upstream) {
                if (predicate.test(item)) {
                    return false;
                }
            }
        }
        return true;
    }

    //#endregion

    //#region Aggregation

    /**
     * Returns the total number of elements in this flow.
     *
     * @return element count
     */
    default long count() {
        var count = 0L;
        try (var upstream = open()) {
            for (var ignored : upstream) {
                count++;
            }
        }
        return count;
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
        try (var upstream = open()) {
            var iter = upstream.iterator();

            if (!iter.hasNext()) {
                throw new NoSuchElementException("Flow is empty");
            }

            var value = iter.next();

            while (iter.hasNext()) {
                value = reducer.apply(value, iter.next());
            }

            return value;
        }
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
        var result = initial;
        try (var upstream = open()) {
            for (var item : upstream) {
                result = accumulator.apply(result, item);
            }
        }
        return result;
    }

    //#endregion

    //#region Collection

    /**
     * Collects all elements into a list.
     *
     * @return list of all elements
     */
    default List<T> toList() {
        var list = new ArrayList<T>();
        try (var upstream = open()) {
            for (var item : upstream) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Collects all elements into a linked hash set.
     *
     * @return set of all elements
     */
    default Set<T> toSet() {
        var set = new LinkedHashSet<T>();
        try (var upstream = open()) {
            for (var item : upstream) {
                set.add(item);
            }
        }
        return set;
    }

    /**
     * Collects all elements into a collection created by {@code supplier}.
     *
     * @param supplier collection factory
     * @param <C>      collection type
     * @return the populated collection
     */
    default <C extends Collection<T>> C toCollection(Supplier<C> supplier) {
        var collection = supplier.get();
        try (var upstream = open()) {
            for (var item : upstream) {
                collection.add(item);
            }
        }
        return collection;
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
        try (var upstream = open()) {
            for (var item : upstream) {
                collector.accept(item);
            }
        }
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
        var sequence = open();
        var characteristics = Spliterator.ORDERED | Spliterator.NONNULL;
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(sequence.iterator(), characteristics),
                false
        ).onClose(sequence::close);
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
