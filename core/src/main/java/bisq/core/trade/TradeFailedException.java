package bisq.core.trade;

public class TradeFailedException extends RuntimeException {
    public TradeFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
