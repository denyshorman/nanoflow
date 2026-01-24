package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.time.Duration;

public class IntervalFlow extends BufferedFlow<Long> {
    public IntervalFlow(Duration initialDelay, Duration period) {
        super(new Action(initialDelay, period));
    }

    private record Action(Duration initialDelay, Duration period) implements Flow.Action<Long> {
        public void perform(Emitter<Long> emitter) throws InterruptedException {
            if (!initialDelay.isZero()) {
                Thread.sleep(initialDelay);
            }

            var tick = 0L;

            while (!Thread.currentThread().isInterrupted()) {
                emitter.emit(tick++);
                Thread.sleep(period);
            }
        }
    }
}
