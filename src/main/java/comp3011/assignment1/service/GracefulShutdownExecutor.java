package comp3011.assignment1.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

//Handles real shutdowns by waiting briefly for the 202 to be sent, then closes Spring and exits.
@Service
public class GracefulShutdownExecutor implements ShutdownExecutor {

    private final ApplicationContext applicationContext;

    public GracefulShutdownExecutor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void initiateShutdown() {
        CompletableFuture.runAsync(() -> {
            try {
            	// give the 202 response time to send before the app closes
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        });
    }
}
