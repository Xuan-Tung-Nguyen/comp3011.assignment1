package comp3011.assignment1.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

//Push mutable state into service so controller become stateless and guard logic becomes independently testable
@Service
public class ShutdownService {

    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final ShutdownExecutor shutdownExecutor;

    public ShutdownService(ShutdownExecutor shutdownExecutor) {
        this.shutdownExecutor = shutdownExecutor;
    }

    //CompareAndSet guarantees only one caller ever wins, even under concurrent requests
    public boolean requestShutdown() {
        if (!shutdownRequested.compareAndSet(false, true)) {
            return false;
        }
        shutdownExecutor.initiateShutdown();
        return true;
    }
}
