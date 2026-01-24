package io.github.denyshorman.nanoflow.internal.flow.producer;

import io.github.denyshorman.nanoflow.Flow;

public class RangeInclusiveFlow extends BufferedFlow<Integer> {
    public RangeInclusiveFlow(int start, int end) {
        super(new Action(start, end));
    }

    private record Action(int start, int end) implements Flow.Action<Integer> {
        public void perform(Emitter<Integer> emitter) throws InterruptedException {
            for (long i = start; i <= end; i++) {
                emitter.emit((int) i);
            }
        }
    }
}
