package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FromTest {
    @Nested
    class IterableSource {
        @Test
        void shouldCreateFromIterable() {
            var flow = Flows.from(List.of(1, 2, 3));

            assertEquals(List.of(1, 2, 3), flow.toList());
        }
    }

    @Nested
    class StreamSource {
        @Test
        void shouldCreateFromStreamAndCloseIt() {
            var closed = new AtomicBoolean(false);
            var stream = Stream.of(1, 2, 3).onClose(() -> closed.set(true));
            var flow = Flows.from(stream);

            assertEquals(List.of(1, 2, 3), flow.toList());
            assertTrue(closed.get());
        }
    }

    @Nested
    class BlockingQueueSource {
        @Test
        void shouldCreateFromBlockingQueue() {
            var queue = new ArrayBlockingQueue<Integer>(3);
            queue.add(1);
            queue.add(2);
            queue.add(3);

            var values = Flows.from(queue).take(queue.size()).toList();

            assertEquals(List.of(1, 2, 3), values);
        }

        @Test
        void shouldStopOnStopToken() {
            var queue = new ArrayBlockingQueue<String>(3);
            queue.add("a");
            queue.add("b");
            queue.add("stop");

            var values = Flows.from(queue, "stop").toList();

            assertEquals(List.of("a", "b"), values);
        }

        @Test
        void shouldStopOnPredicate() {
            var queue = new ArrayBlockingQueue<Integer>(3);
            queue.add(1);
            queue.add(2);
            queue.add(0);

            var values = Flows.from(queue, item -> item == 0).toList();

            assertEquals(List.of(1, 2), values);
        }
    }

    @Nested
    class PublisherSource {
        @Test
        void shouldCreateFlowFromPublisher() {
            var publisher = new TestPublisher<>(List.of(1, 2, 3));
            var flow = Flows.from(publisher);

            assertEquals(List.of(1, 2, 3), flow.toList());
        }

        private record TestPublisher<T>(List<T> items) implements Flow.Publisher<T> {
            @Override
            public void subscribe(Flow.Subscriber<? super T> subscriber) {
                subscriber.onSubscribe(new Flow.Subscription() {
                    private int index = 0;
                    private boolean cancelled = false;
                    private boolean completed = false;

                    @Override
                    public synchronized void request(long n) {
                        if (cancelled || completed) {
                            return;
                        }

                        if (n <= 0) {
                            completed = true;
                            subscriber.onError(new IllegalArgumentException("request must be positive"));
                            return;
                        }

                        var remaining = n;

                        while (remaining > 0 && index < items.size()) {
                            subscriber.onNext(items.get(index++));
                            remaining--;
                        }

                        if (index >= items.size() && !completed) {
                            completed = true;
                            subscriber.onComplete();
                        }
                    }

                    @Override
                    public synchronized void cancel() {
                        cancelled = true;
                    }
                });
            }
        }
    }
}
