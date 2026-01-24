package io.github.denyshorman.nanoflow.internal.sequence.producer;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.denyshorman.nanoflow.internal.util.SneakyThrow.sneakyThrow;

public class PublisherSequence<T> extends BufferedSequence<T> {
    public PublisherSequence(Publisher<? extends T> publisher) {
        super(new Action<>(publisher));
    }

    private record Action<T>(Publisher<? extends T> publisher) implements Flow.Action<T> {
        @Override
        public void perform(Flow.Emitter<T> emitter) throws Exception {
            var queue = new LinkedTransferQueue<Event>();
            var subscriptionRef = new AtomicReference<@Nullable Subscription>();
            var requested = new AtomicBoolean(false);

            try {
                publisher.subscribe(new Subscriber<T>() {
                    public void onSubscribe(Subscription subscription) {
                        if (!subscriptionRef.compareAndSet(null, subscription)) {
                            subscription.cancel();
                            return;
                        }

                        requested.set(true);
                        subscription.request(1);
                    }

                    public void onNext(T item) {
                        if (!requested.compareAndSet(true, false)) {
                            var subscription = subscriptionRef.get();

                            if (subscription != null) {
                                subscription.cancel();
                            }

                            queue.offer(new OnError(new IllegalStateException("Publisher emitted without demand")));

                            return;
                        }

                        queue.offer(new OnNext(item));
                    }

                    public void onError(Throwable throwable) {
                        queue.offer(new OnError(throwable));
                    }

                    public void onComplete() {
                        queue.offer(new OnComplete());
                    }
                });

                while (true) {
                    Event event;

                    try {
                        event = queue.take();
                    } catch (InterruptedException e) {
                        var subscription = subscriptionRef.get();
                        if (subscription != null) {
                            subscription.cancel();
                        }
                        throw e;
                    }

                    switch (event) {
                        case OnNext onNext -> {
                            @SuppressWarnings("unchecked")
                            var value = (T) onNext.value();
                            emitter.emit(value);

                            var subscription = subscriptionRef.get();

                            if (subscription != null) {
                                requested.set(true);
                                subscription.request(1);
                            }
                        }
                        case OnError(var throwable) -> throw sneakyThrow(throwable);
                        case OnComplete() -> {
                            return;
                        }
                    }
                }
            } catch (Throwable t) {
                if (t instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw sneakyThrow(t);
            } finally {
                var subscription = subscriptionRef.get();
                if (subscription != null) {
                    subscription.cancel();
                }
            }
        }
    }

    private sealed interface Event permits OnNext, OnError, OnComplete {
    }

    private record OnNext(Object value) implements Event {
    }

    private record OnError(Throwable throwable) implements Event {
    }

    private record OnComplete() implements Event {
    }
}
