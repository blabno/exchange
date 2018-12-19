package bisq.httpapi.facade;

import java.util.concurrent.CompletableFuture;

public class FacadeUtil {

    public static <T> CompletableFuture<T> failFuture(CompletableFuture<T> futureResult, Throwable throwable) {
        futureResult.completeExceptionally(throwable);
        return futureResult;
    }
}
