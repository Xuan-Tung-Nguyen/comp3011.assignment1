package comp3011.assignment1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.service.ShutdownExecutor;
import comp3011.assignment1.service.ShutdownService;

class ShutdownServiceTest {

    //Verify only first shutdown request is accepted & shutdown is triggered only once
    @Test
    void onlyFirstRequestWinsAndInitiatesShutdown() {
        ShutdownExecutor executor = mock(ShutdownExecutor.class);
        ShutdownService service = new ShutdownService(executor);

        assertThat(service.requestShutdown()).isTrue();
        assertThat(service.requestShutdown()).isFalse();
        assertThat(service.requestShutdown()).isFalse();

        verify(executor, times(1)).initiateShutdown();
    }

    //Verify that concurrent shutdown requests are handled carefully with one winner
    @Test
    void exactlyOneThreadWinsUnderConcurrentShutdownRequests() throws InterruptedException {
        ShutdownExecutor executor = mock(ShutdownExecutor.class);
        ShutdownService service = new ShutdownService(executor);

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger winners = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    //Maximize contention by making all threads attempt shutdown at the same time.
                    startGate.await();

                    if (service.requestShutdown()) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();

        //Waits for all worker threads to finish before checking the result
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        //Verifies the thread-safe guard allows exactly one request through
        assertThat(winners.get()).isEqualTo(1);

        //Confirms shutdown is initiated exactly once despite concurrent requests
        verify(executor, times(1)).initiateShutdown();
    }
}
