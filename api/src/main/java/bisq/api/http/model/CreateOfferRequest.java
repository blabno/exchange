package bisq.api.http.model;

import bisq.api.http.model.validation.StringEnumeration;

import bisq.core.offer.OfferPayload;

import java.math.BigDecimal;



import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public class CreateOfferRequest {

    @NotEmpty
    public String accountId;

    @Schema(allowableValues = {"BUY", "SELL"})
    @NotNull
    @StringEnumeration(enumClass = OfferPayload.Direction.class)
    public String direction;

    @Schema(allowableValues = {"FIXED", "PERCENTAGE"})
    @NotNull
    @StringEnumeration(enumClass = PriceType.class)
    public String priceType;

    @NotEmpty
    public String marketPair;

    public BigDecimal percentageFromMarketPrice;

    @Schema(description = "In case of fiat it is amount of cents multiplied by 100, i.e. 1 EUR is 10000. In case of altcoins it is amount of satoshis, i.e. 1 BTC is 100000000.")
    @Min(0)
    public long fixedPrice;

    @Min(1)
    public long amount;

    @Min(1)
    public long minAmount;

    @Min(1)
    public Long buyerSecurityDeposit;
}
