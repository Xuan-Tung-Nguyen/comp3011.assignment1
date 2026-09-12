package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import comp3011.assignment1.dto.ErrorResponse;
import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.dto.ShutDownResponse;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final Instant serverStart = Instant.now();
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false); 
    private final ApplicationContext context;
    public AdminController(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }
    //prevent concurrent shutdown requests
    
    @GetMapping("/uptime")
    public UptimeResponse getUptime() {
        Instant now = Instant.now();
        double seconds = Duration.between(serverStart, now).toMillis() / 1000.0;
        return new UptimeResponse(serverStart.toString(), now.toString(), seconds);
        
    }
    
    @PostMapping("/shutdown")
    public ResponseEntity<?> shutdown(
            HttpServletRequest request) {

        if (!shuttingDown.compareAndSet(false, true)) {

            ErrorResponse conflict =
                    new ErrorResponse(
                            Instant.now().toString(),
                            409,
                            "Conflict",
                            "Graceful shutdown is already in progress.",
                            request.getRequestURI()
                    );

            return ResponseEntity.status(409).body(conflict);
        }
        
     //Run the shutdown logic asynchronously so the current thread is not blocked.
        CompletableFuture.runAsync(() -> {

            try {
            	//Allow the HTTP/202 response 500ms to be sent to the client.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //Restore the interrupted status and stop the shutdown task.
                Thread.currentThread().interrupt();
                return;
            }

            //Close Spring application context and return exit code 0.
            int exitCode =
                    SpringApplication.exit(
                            context,
                            () -> 0
                    );

            //Use exit code to terminate JVM
            System.exit(exitCode);
        });

        
        return ResponseEntity
                .accepted()
                .body(
                    new ShutDownResponse(
                        "Graceful shutdown requested."
                    )
                );
    }
}

