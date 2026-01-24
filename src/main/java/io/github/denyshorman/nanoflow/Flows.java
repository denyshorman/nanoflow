package io.github.denyshorman.nanoflow;

import io.github.denyshorman.nanoflow.internal.flow.producer.*;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Factory and generator methods for {@link Flow}.
 *
 * <p>This class provides static methods to create flows from various sources like
 * collections, streams, or custom producer actions.
 *
 * <p>Nanoflow does not allow {@code null} values in flows. All methods in this class
 * expect non-null elements and producers. Providing {@code null} where it's not
 * explicitly allowed by {@link org.jspecify.annotations.Nullable} will result
 * in a {@link NullPointerException}.
 *
 * <p>Typical usage:
 * <pre>{@code
 * var flow = Flows.of(1, 2, 3);
 * flow.collect(System.out::println);
 * }</pre>
 */
public final class Flows {
    private Flows() {
    }

    //#region Core

    /**
     * Creates a flow from a producer action.
     *
     * <p>This is the primary way to create custom flows. The {@code action} defines how values
     * are produced and is executed each time the flow is {@link Flow#open() opened}.
     *
     * <h3>Execution Model</h3>
     * Each call to {@link Flow#open()} (or terminal operators like {@link Flow#collect(Flow.Collector)})
     * triggers a new execution of the {@code action}.
     *
     * <h3>Buffer and Backpressure</h3>
     * By default, this method uses a <b>synchronous handoff</b> (buffer size 0). The producer
     * will block on {@link Flow.Emitter#emit(Object)} until the consumer retrieves the value.
     * To use a buffer, use {@link #flow(int, Flow.Action)}.
     *
     * <h3>Cancellation</h3>
     * If the consumer closes the flow or is interrupted, the producer thread is interrupted.
     * Blocking calls that honor interruption may throw {@link InterruptedException} or return early.
     * Producers should handle interruption or periodically check {@link Thread#isInterrupted()}
     * (especially if CPU-bound) to stop production and release resources.
     *
     * <h3>Exception Handling</h3>
     * Exceptions thrown within the {@code action} are propagated directly to the consumer
     * during iteration or collection.
     *
     * <h3>Concurrent Emission</h3>
     * The {@code action} can emit values from multiple threads concurrently using the
     * provided {@link Flow.Emitter}. The consumer sees a single sequence in arrival order.
     *
     * @param action producer action
     * @param <T>    value type
     * @return a new flow
     */
    public static <T> Flow<T> flow(Flow.Action<T> action) {
        return new BufferedFlow<>(action);
    }

    /**
     * Creates a flow from a producer action using a buffer of the given size.
     *
     * <p>This method behaves identically to {@link #flow(Flow.Action)}, but allows
     * specifying a buffer capacity.
     *
     * <h3>Buffer and Backpressure</h3>
     * A buffer allows the producer to continue emitting values even if the consumer is slower.
     * When the buffer is full, the producer will block on {@link Flow.Emitter#emit(Object)}
     * until the consumer makes more space.
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
     * @param action     producer action that will be executed when the flow is opened
     * @param <T>        value type
     * @return a new flow
     * @throws IllegalArgumentException if {@code bufferSize} is negative
     */
    public static <T> Flow<T> flow(int bufferSize, Flow.Action<T> action) {
        return new BufferedFlow<>(bufferSize, action);
    }
    //#endregion

    //#region Lifecycle

    /**
     * Returns a flow that emits no values and completes immediately.
     *
     * @param <T> value type
     * @return an empty flow
     */
    public static <T> Flow<T> empty() {
        return EmptyFlow.instance();
    }

    /**
     * Returns a flow that never emits and never completes.
     *
     * @param <T> value type
     * @return a never-ending flow
     */
    public static <T> Flow<T> never() {
        return new NeverFlow<>();
    }

    /**
     * Returns a flow that fails with {@code error} when collected.
     *
     * @param error error to throw
     * @param <T>   value type
     * @return a failing flow
     */
    public static <T> Flow<T> error(Throwable error) {
        return new ErrorFlow<>(error);
    }

    /**
     * Returns a flow that calls {@code supplier} for each subscription.
     *
     * @param supplier flow supplier
     * @param <T>      value type
     * @return a deferred flow
     */
    public static <T> Flow<T> defer(Supplier<? extends Flow<? extends T>> supplier) {
        return new DeferFlow<>(supplier);
    }

    //#endregion

    //#region Composition

    /**
     * Concatenates the given flows in order.
     *
     * @param flows flows to concatenate
     * @param <T>   value type
     * @return a concatenated flow
     */
    @SafeVarargs
    public static <T> Flow<T> concat(Flow<? extends T>... flows) {
        return new ConcatFlow<>(flows);
    }

    //#endregion

    //#region Sources

    //#region Iterable & Stream

    /**
     * Returns a flow that emits values from the given iterable.
     *
     * <p>The iterable is traversed each time the flow is opened.
     *
     * @param items source items. Must not be null.
     * @param <T>   value type
     * @return a flow over the iterable
     */
    public static <T> Flow<T> from(Iterable<? extends T> items) {
        return new IterableFlow<>(items);
    }

    /**
     * Returns a flow that emits values from the given stream and closes it.
     *
     * <p>The stream is consumed each time the flow is opened. Since streams can only be
     * consumed once, this flow can only be opened once. For a flow that can be opened
     * multiple times from a stream source, use {@link #defer(Supplier)} and provide
     * a stream supplier.
     *
     * @param stream source stream. Must not be null.
     * @param <T>    value type
     * @return a flow over the stream
     */
    public static <T> Flow<T> from(Stream<? extends T> stream) {
        return new StreamFlow<>(stream);
    }

    /**
     * Returns a flow that emits values from the given publisher.
     *
     * <p>The publisher is subscribed each time the flow is opened. If the publisher only
     * supports a single subscription, this flow can be opened only once.
     *
     * @param publisher source publisher. Must not be null.
     * @param <T>       value type
     * @return a flow over the publisher
     */
    public static <T> Flow<T> from(Publisher<? extends T> publisher) {
        return new PublisherFlow<>(publisher);
    }

    //#endregion

    //#region BlockingQueue

    /**
     * Returns a flow that emits values taken from {@code queue}.
     *
     * <p>This method blocks on {@link BlockingQueue#take()} and stops on interruption.
     * Note that if the queue is being populated by another thread, this flow might
     * complete before all elements are produced if the queue becomes temporarily empty
     * and the thread is interrupted, or if it is used in a context that expects a
     * termination signal. For queues with a termination signal, use {@link #from(BlockingQueue, Object)}.
     *
     * @param queue source queue. Must not be null.
     * @param <T>   value type
     * @return a flow over the queue
     */
    public static <T> Flow<T> from(BlockingQueue<? extends T> queue) {
        return new BlockingQueueFlow<>(queue);
    }

    /**
     * Returns a flow that emits values from {@code queue} until {@code stopToken} is seen.
     *
     * <p>The {@code stopToken} is not emitted. This method blocks while waiting for elements
     * in the queue.
     *
     * @param queue     source queue. Must not be null.
     * @param stopToken value that signals the end of the flow
     * @param <T>       value type
     * @return a flow over the queue
     */
    public static <T> Flow<T> from(BlockingQueue<? extends T> queue, T stopToken) {
        return new BlockingQueueFlow<>(queue, item -> Objects.equals(item, stopToken));
    }

    /**
     * Returns a flow that emits values from {@code queue} until {@code isStop} is true.
     *
     * <p>The element that satisfies {@code isStop} is not emitted. This method blocks while
     * waiting for elements in the queue.
     *
     * @param queue  source queue. Must not be null.
     * @param isStop predicate that signals the end of the flow
     * @param <T>    value type
     * @return a flow over the queue
     */
    public static <T> Flow<T> from(BlockingQueue<? extends T> queue, Predicate<? super T> isStop) {
        return new BlockingQueueFlow<>(queue, isStop);
    }

    //#endregion

    //#region Array

    /**
     * Returns a flow that emits the given items.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.of("Hello", "World").toList(); // ["Hello", "World"]
     * }</pre>
     *
     * @param items items to emit
     * @param <T>   value type
     * @return a flow over the array
     */
    @SafeVarargs
    public static <T> Flow<T> of(T... items) {
        return items.length == 0 ? empty() : new ArrayFlow<>(items);
    }

    //#endregion

    //#endregion

    //#region Ranges

    /**
     * Returns a flow that emits values from {@code startInclusive} to {@code endExclusive}.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.range(0, 5)
     *     .toList(); // [0, 1, 2, 3, 4]
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endExclusive   the (exclusive) upper bound
     * @return a flow over the range
     */
    public static Flow<Integer> range(int startInclusive, int endExclusive) {
        return new RangeExclusiveFlow(startInclusive, endExclusive);
    }

    /**
     * Returns a flow that emits values from {@code startInclusive} to {@code endInclusive}.
     *
     * @param startInclusive the (inclusive) initial value
     * @param endInclusive   the (inclusive) upper bound
     * @return a flow over the closed range
     */
    public static Flow<Integer> rangeClosed(int startInclusive, int endInclusive) {
        return new RangeInclusiveFlow(startInclusive, endInclusive);
    }

    //#endregion

    //#region Generation

    /**
     * Returns an infinite flow that generates values using the given supplier.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.generate(() -> Math.random())
     *     .take(3)
     *     .toList(); // [0.123..., 0.456..., 0.789...]
     * }</pre>
     *
     * @param supplier value supplier. Must not be null.
     * @param <T>      value type
     * @return a generated flow
     */
    public static <T> Flow<T> generate(Supplier<? extends T> supplier) {
        return new GenerateFlow<>(supplier);
    }

    /**
     * Returns a flow that generates at most {@code count} values using the given supplier.
     *
     * @param count    number of values to generate. Must be non-negative.
     * @param supplier value supplier. Must not be null.
     * @param <T>      value type
     * @return a generated flow
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public static <T> Flow<T> generate(long count, Supplier<? extends T> supplier) {
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");
        if (count == 0) return empty();

        return new GenerateCountFlow<>(count, supplier);
    }

    //#endregion

    //#region Iteration

    /**
     * Returns an infinite flow by applying {@code next} to the previous value, starting with {@code seed}.
     *
     * <p>Example:
     * <pre>{@code
     * Flows.iterate(1, n -> n * 2)
     *     .take(5)
     *     .toList(); // [1, 2, 4, 8, 16]
     * }</pre>
     *
     * @param seed initial value
     * @param next function that produces the next value. Must not be null.
     * @param <T>  value type
     * @return an iterative flow
     */
    public static <T> Flow<T> iterate(T seed, UnaryOperator<T> next) {
        return new IterateFlow<>(seed, next);
    }

    /**
     * Returns a flow by applying {@code next} to the previous value while {@code hasNext} is true.
     *
     * @param seed    initial value
     * @param hasNext predicate that determines if the flow should continue. Must not be null.
     * @param next    function that produces the next value. Must not be null.
     * @param <T>     value type
     * @return an iterative flow
     */
    public static <T> Flow<T> iterate(
            T seed,
            Predicate<? super T> hasNext,
            UnaryOperator<T> next
    ) {
        return new IterateWhileFlow<>(seed, hasNext, next);
    }

    //#endregion

    //#region Repetition

    /**
     * Returns an infinite flow that repeats {@code value}.
     *
     * @param value value to repeat
     * @param <T>   value type
     * @return a repeating flow
     */
    public static <T> Flow<T> repeat(T value) {
        return new RepeatFlow<>(value);
    }

    /**
     * Returns a flow that repeats {@code value} at most {@code count} times.
     *
     * @param count number of times to repeat. Must be non-negative.
     * @param value value to repeat
     * @param <T>   value type
     * @return a repeating flow
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public static <T> Flow<T> repeat(long count, T value) {
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");

        return new RepeatCountFlow<>(count, value);
    }

    //#endregion

    //#region Timing

    /**
     * Returns a flow that emits a single tick after {@code delay} and then completes.
     *
     * @param delay delay before the tick. Must be non-negative.
     * @return a flow that emits a single tick value of 0
     * @throws IllegalArgumentException if {@code delay} is negative
     */
    public static Flow<Long> timer(Duration delay) {
        if (delay.isNegative()) throw new IllegalArgumentException("delay must be >= 0");

        return new TimerFlow(delay);
    }

    /**
     * Returns a flow that emits ticks every {@code period}.
     *
     * <p>The first tick is emitted immediately, then every {@code period} thereafter.
     *
     * @param period time between ticks. Must be positive.
     * @return a flow of ticks starting at 0
     * @throws IllegalArgumentException if {@code period} is zero or negative
     */
    public static Flow<Long> interval(Duration period) {
        return interval(Duration.ZERO, period);
    }

    /**
     * Returns a flow that emits ticks after {@code initialDelay} and then every {@code period}.
     *
     * @param initialDelay delay before the first tick. Must be non-negative.
     * @param period       time between ticks. Must be positive.
     * @return a flow of ticks starting at 0
     * @throws IllegalArgumentException if {@code initialDelay} is negative or {@code period} is zero or negative
     */
    public static Flow<Long> interval(Duration initialDelay, Duration period) {
        if (initialDelay.isNegative()) throw new IllegalArgumentException("initialDelay must be >= 0");
        if (period.isZero() || period.isNegative()) throw new IllegalArgumentException("period must be > 0");

        return new IntervalFlow(initialDelay, period);
    }

    //#endregion
}
