package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

import java.time.Duration;

public class TimerFlow extends BufferedFlow<Long> {
    public TimerFlow(Duration delay) {
        super(new Action(delay));
    }

    private record Action(Duration delay) implements Flow.Action<Long> {
        public void perform(Emitter<Long> emitter) throws InterruptedException {
            if (!delay.isZero()) {
                Thread.sleep(delay);
            }
            emitter.emit(0L);
        }
    }
}
