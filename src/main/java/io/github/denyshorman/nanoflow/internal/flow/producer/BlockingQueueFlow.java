package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;

public class BlockingQueueFlow<T> extends BufferedFlow<T> {
    public BlockingQueueFlow(BlockingQueue<? extends T> queue) {
        this(queue, null);
    }

    public BlockingQueueFlow(
            BlockingQueue<? extends T> queue,
            @Nullable Predicate<? super T> isStop
    ) {
        super(new Action<>(queue, isStop));
    }

    private record Action<T>(
            BlockingQueue<? extends T> queue,
            @Nullable Predicate<? super T> isStop
    ) implements Flow.Action<T> {
        public void perform(Emitter<T> emitter) throws InterruptedException {
            while (!Thread.currentThread().isInterrupted()) {
                var item = queue.take();
                if (isStop != null && isStop.test(item)) {
                    return;
                }
                emitter.emit(item);
            }
        }
    }
}
