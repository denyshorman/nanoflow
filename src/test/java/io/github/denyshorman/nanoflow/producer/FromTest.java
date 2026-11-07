package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FromTest {
    @Nested
    class IterableSource {
        @Test
        void shouldCreateFromIterable() {
            var flow = Flows.from(List.of(1, 2, 3));

            assertEquals(List.of(1, 2, 3), flow.toList());
        }

        @Test
        void shouldAllowMultipleIterationsOverIterable() {
            var flow = Flows.from(List.of("a", "b"));

            assertEquals(List.of("a", "b"), flow.toList());
            assertEquals(List.of("a", "b"), flow.toList());
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

        @Test
        void shouldCloseUnderlyingStreamWhenStoppedEarly() {
            var closed = new AtomicBoolean(false);
            var stream = Stream.of(1, 2, 3, 4).onClose(() -> closed.set(true));
            var flow = Flows.from(stream);

            assertEquals(List.of(1, 2), flow.take(2).toList());
            assertTrue(closed.get());
        }

        @Test
        void shouldFailWhenStreamFlowIsOpenedTwice() {
            var flow = Flows.from(Stream.of(1, 2));

            assertEquals(List.of(1, 2), flow.toList());
            assertThrows(IllegalStateException.class, flow::toList);
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
        void shouldReturnEmptyWhenStopTokenIsFirstElement() {
            var queue = new ArrayBlockingQueue<String>(2);
            queue.add("stop");
            queue.add("after");

            assertEquals(List.of(), Flows.from(queue, "stop").toList());
            assertEquals(List.of("after"), List.copyOf(queue));
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

        @Test
        void shouldPropagatePredicateException() {
            var queue = new ArrayBlockingQueue<Integer>(2);
            queue.add(1);

            var flow = Flows.from(queue, item -> {
                throw new IllegalStateException("predicate boom");
            });

            assertThrows(IllegalStateException.class, flow::toList);
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

        @Test
        void shouldPropagatePublisherOnError() {
            Flow.Publisher<Integer> publisher = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onError(new IOException("publisher boom"));
                }

                @Override
                public void cancel() {
                }
            });

            assertThrows(IOException.class, () -> Flows.from(publisher).toList());
        }

        @Test
        void shouldCancelSubscriptionWhenConsumerStopsEarly() throws InterruptedException {
            var cancelled = new CountDownLatch(1);

            Flow.Publisher<Integer> publisher = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
                private int next = 1;

                @Override
                public void request(long n) {
                    subscriber.onNext(next++);
                }

                @Override
                public void cancel() {
                    cancelled.countDown();
                }
            });

            assertEquals(List.of(1, 2), Flows.from(publisher).take(2).toList());
            assertTrue(cancelled.await(5, TimeUnit.SECONDS));
        }

        @Test
        void shouldFailAndCancelWhenPublisherEmitsWithoutDemand() throws InterruptedException {
            var cancelled = new CountDownLatch(1);

            Flow.Publisher<Integer> publisher = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onNext(1);
                    subscriber.onNext(2); // Unrequested second item
                }

                @Override
                public void cancel() {
                    cancelled.countDown();
                }
            });

            assertThrows(IllegalStateException.class, () -> Flows.from(publisher).toList());
            assertTrue(cancelled.await(5, TimeUnit.SECONDS));
        }

        @Test
        void shouldCancelDuplicateOnSubscribe() {
            var duplicateCancelled = new AtomicBoolean(false);

            Flow.Publisher<Integer> publisher = subscriber -> {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        subscriber.onNext(1);
                        subscriber.onComplete();
                    }

                    @Override
                    public void cancel() {
                    }
                });

                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                    }

                    @Override
                    public void cancel() {
                        duplicateCancelled.set(true);
                    }
                });
            };

            assertEquals(List.of(1), Flows.from(publisher).toList());
            assertTrue(duplicateCancelled.get());
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
