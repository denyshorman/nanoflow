package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.sequence.producer.StateSequence;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class StateFlow<T> implements Flow.MutableState<T> {
    private final AtomicReference<T> value;
    private final Set<StateSequence<T>> subscribers = ConcurrentHashMap.newKeySet();

    public StateFlow(T initialValue) {
        value = new AtomicReference<>(initialValue);
    }

    @Override
    public T get() {
        return value.get();
    }

    @Override
    public void set(T value) {
        var prev = this.value.getAndSet(value);

        if (!Objects.equals(prev, value)) {
            notifySubscribers();
        }
    }

    @Override
    public boolean compareAndSet(T expect, T update) {
        while (true) {
            var current = value.get();

            if (!Objects.equals(current, expect)) {
                return false;
            }

            if (value.compareAndSet(current, update)) {
                if (!Objects.equals(current, update)) {
                    notifySubscribers();
                }

                return true;
            }
        }
    }

    @Override
    public T update(UnaryOperator<T> function) {
        while (true) {
            var prev = value.get();
            var next = function.apply(prev);

            if (value.compareAndSet(prev, next)) {
                if (!Objects.equals(prev, next)) {
                    notifySubscribers();
                }

                return next;
            }
        }
    }

    @Override
    public Sequence<T> open() {
        return new StateSequence<>(value::get, subscribers);
    }

    private void notifySubscribers() {
        for (var subscriber : subscribers) {
            subscriber.wakeUp();
        }
    }
}
