package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.service.TokenUsageTracker;

@RestController
@RequestMapping("/api/v1/global")
public class StatsController {

    private final TokenUsageTracker tracker;

    public StatsController(TokenUsageTracker tracker) {
        this.tracker = tracker;
    }

    @GetMapping("/stats")
    public GlobalStatsResponse getStats() {
        return new GlobalStatsResponse(
                tracker.getInputTokens(),
                tracker.getOutputTokens()
        );
    }
}
