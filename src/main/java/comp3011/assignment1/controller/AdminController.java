package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import comp3011.assignment1.dto.UptimeResponse;


@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final Instant serverStart = Instant.now();

    @GetMapping("/uptime")
    public UptimeResponse getUptime() {
        Instant now = Instant.now();
        double seconds = Duration.between(serverStart, now).toMillis() / 1000.0;
        return new UptimeResponse(serverStart.toString(), now.toString(), seconds);
    }
}