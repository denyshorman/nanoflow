package io.github.denyshorman.nanoflow.producer;

import io.github.denyshorman.nanoflow.Flows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NeverTest {
    @Test
    void shouldCompleteWhenTakenZero() {
        var values = Flows.<Integer>never().take(0).toList();

        assertEquals(List.of(), values);
    }

    @Test
    void shouldUnblockAndPreserveInterruptFlagWhenConsumerIsInterrupted() throws InterruptedException {
        var started = new CountDownLatch(1);
        var interruptedFlag = new CountDownLatch(1);

        var consumer = Thread.ofVirtual().start(() -> {
            assertThrows(InterruptedException.class, () -> {
                try (var items = Flows.<Integer>never().open()) {
                    started.countDown();
                    for (var ignored : items) {
                        break;
                    }
                }
            });

            if (Thread.currentThread().isInterrupted()) {
                interruptedFlag.countDown();
            }
        });

        assertTrue(started.await(5, TimeUnit.SECONDS));
        Thread.sleep(15);
        consumer.interrupt();

        assertTrue(interruptedFlag.await(5, TimeUnit.SECONDS));
        consumer.join(5000);
    }
}
