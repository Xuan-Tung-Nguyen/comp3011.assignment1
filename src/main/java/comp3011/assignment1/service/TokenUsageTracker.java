package comp3011.assignment1.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class TokenUsageTracker {

    private final AtomicLong inputTokens = new AtomicLong(0);
    private final AtomicLong outputTokens = new AtomicLong(0);

    public void record(long input, long output) {
        inputTokens.addAndGet(input);
        outputTokens.addAndGet(output);
    }

    public long getInputTokens() { return inputTokens.get(); }
    public long getOutputTokens() { return outputTokens.get(); }
}

