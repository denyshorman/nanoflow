package io.github.denyshorman.nanoflow.internal.flow.operator.intermediate;

import io.github.denyshorman.nanoflow.Flow;
import io.github.denyshorman.nanoflow.internal.flow.producer.BufferedFlow;

import java.util.function.BiFunction;

public class ZipFlow<T, U, R> extends BufferedFlow<R> {
    public ZipFlow(
            Flow<? extends T> upstream,
            Flow<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> zipper
    ) {
        super(new Action<>(upstream, other, zipper));
    }

    private record Action<T, U, R>(
            Flow<? extends T> upstream,
            Flow<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> zipper
    ) implements Flow.Action<R> {
        public void perform(Emitter<R> emitter) throws Exception {
            try (var left = upstream.open(); var right = other.open()) {
                var leftIter = left.iterator();
                var rightIter = right.iterator();
                while (leftIter.hasNext() && rightIter.hasNext()) {
                    var zipped = zipper.apply(leftIter.next(), rightIter.next());
                    emitter.emit(zipped);
                }
            }
        }
    }
}
