package comp3011.assignment1.service;

//Make the AdminController testable without terminating test JVM by extract shutdown behind interface
public interface ShutdownExecutor {

    void initiateShutdown();
}
