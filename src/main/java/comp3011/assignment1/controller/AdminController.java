package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import comp3011.assignment1.dto.ErrorResponse;
import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.service.ShutdownService;
import comp3011.assignment1.dto.ShutDownResponse;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final Instant serverStart = Instant.now();
    private final ShutdownService shutdownService;

    public AdminController(ShutdownService shutdownService) {
        this.shutdownService = shutdownService;
    }

    //prevent concurrent shutdown requests
    
    //Give information about how long the server has been running by calculate utcServerStart, utcNow, and serverUptimeSeconds
    @GetMapping("/uptime")
    public UptimeResponse getUptime() {
        Instant now = Instant.now();
        double seconds = Duration.between(serverStart, now).toMillis() / 1000.0;
        return new UptimeResponse(serverStart.toString(), now.toString(), seconds);
    }
    
    //request a graceful shutdown of the server
    @PostMapping("/shutdown")
    public ResponseEntity<?> shutdown(HttpServletRequest request) {
        if (!shutdownService.requestShutdown()) {
            ErrorResponse conflict = new ErrorResponse(
                Instant.now().toString(), 409, "Conflict",
                "Graceful shutdown is already in progress.", request.getRequestURI());
            return ResponseEntity.status(409).body(conflict);
        }

        //Return shutdown response immdediately while shutdown happens asynchronously
        return ResponseEntity.accepted().body(new ShutDownResponse("Graceful shutdown requested."));
    }
}