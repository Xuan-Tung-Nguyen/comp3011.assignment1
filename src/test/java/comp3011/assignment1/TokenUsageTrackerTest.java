package comp3011.assignment1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.service.TokenUsageTracker;

class TokenUsageTrackerTest {

    @Test
    void recordIsThreadSafeUnderConcurrentUpdates() throws InterruptedException {
        TokenUsageTracker tracker = new TokenUsageTracker();
        
        //Run concurrent updates to expose race conditions
        int threads = 100;
        int callsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        //Each thread records token usage 
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < callsPerThread; j++) {
                        tracker.record(1, 2);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        //Wait for the concurrent updates to finished
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        //Verify no updates were lost
        assertThat(tracker.getInputTokens()).isEqualTo((long) threads * callsPerThread);
        assertThat(tracker.getOutputTokens()).isEqualTo((long) threads * callsPerThread * 2);
    }
}