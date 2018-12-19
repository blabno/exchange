package bisq.httpapi.facade;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShutdownFacade {

    @Setter
    public Runnable shutdownHandler;

    boolean isShutdownSupported() {
        return shutdownHandler != null;
    }

    void shutDown() {
        if (isShutdownSupported()) {
            shutdownHandler.run();
        } else {
            log.warn("Shutdown is not supported");
        }
    }
}
