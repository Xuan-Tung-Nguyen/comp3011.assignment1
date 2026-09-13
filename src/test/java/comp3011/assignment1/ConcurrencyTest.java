package comp3011.assignment1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import comp3011.assignment1.service.TokenUsageTracker;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("stub") //Uses stubbed OpenAI responses
class ConcurrencyLoadTest {

    @LocalServerPort
    int port;

    @Autowired
    //Tracks token usage across concurrent requests
    TokenUsageTracker tracker;

    @Test
    void handles200PlusConcurrentTranscribeRequestsReliably() throws InterruptedException {
    	//Total number of concurrent requests to simulate
        int requestCount = 250; 
        //50 requests at once
        ExecutorService executor = Executors.newFixedThreadPool(50); 
        //Wait for all requests to finish
        CountDownLatch latch = new CountDownLatch(requestCount); 
        //Thread-safe counter for failed requests
        AtomicInteger failures = new AtomicInteger(); 
        
        RestTemplate restTemplate = new RestTemplate();
        //Stub audio payload as no real audio needed
        byte[] fakeAudio = "fake-audio-bytes".getBytes();
        //Capture counter before the load test
        long inputTokensBefore = tracker.getInputTokens(); 
        long start = System.currentTimeMillis();

        //Submit 250 HTTP requests concurrently
        for (int i = 0; i < requestCount; i++) {
            executor.submit(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.valueOf("audio/webm"));

                    HttpEntity<byte[]> entity = new HttpEntity<>(fakeAudio, headers);

                    ResponseEntity<String> response = restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/v1/speech/transcribe",
                        entity,
                        String.class
                    );

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        failures.incrementAndGet();
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                  //Mark request as completed
                } finally {
                    latch.countDown(); 
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS); // Ensure requests don't hang indefinitely
        long elapsed = System.currentTimeMillis() - start;
        executor.shutdown();

        //Check if all requests completed within expected time
        assertThat(completed)
            .as("all requests completed without hanging")
            .isTrue();

        assertThat(failures.get())
            .as("no failed/errored requests")
            .isZero();

        assertThat(elapsed)
            .as("no significant delay under load")
            .isLessThan(10_000);

        //Verify concurrent requests did not cause lost updates to the shared token counter
        assertThat(tracker.getInputTokens() - inputTokensBefore)
            .isEqualTo(requestCount);
    }
}
