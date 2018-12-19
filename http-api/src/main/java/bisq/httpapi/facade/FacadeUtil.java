package bisq.httpapi.facade;

import java.util.concurrent.CompletableFuture;

final class FacadeUtil {

    static <T> CompletableFuture<T> failFuture(CompletableFuture<T> futureResult, Throwable throwable) {
        futureResult.completeExceptionally(throwable);
        return futureResult;
    }
}
