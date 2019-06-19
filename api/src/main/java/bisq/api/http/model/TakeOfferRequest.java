package bisq.api.http.model;

import javax.annotation.Nullable;



import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public class TakeOfferRequest {

    @NotNull
    @Min(1)
    public long amount;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public Long maxFundsForTrade;

    @NotNull
    @NotEmpty
    public String paymentAccountId;


    public TakeOfferRequest() {
    }

    public TakeOfferRequest(String paymentAccountId, long amount) {
        this(paymentAccountId, amount, null);
    }

    public TakeOfferRequest(String paymentAccountId, long amount, @Nullable Long maxFundsForTrade) {
        this.amount = amount;
        this.maxFundsForTrade = maxFundsForTrade;
        this.paymentAccountId = paymentAccountId;
    }

}
