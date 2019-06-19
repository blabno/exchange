package bisq.api.http.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class Market {
    @JsonProperty
    String pair;
    @JsonProperty
    String baseCurrencyCode;
    @JsonProperty
    String counterCurrencyCode;

    public Market(String baseCurrencyCode, String counterCurrencyCode) {
        this.baseCurrencyCode = baseCurrencyCode.toUpperCase();
        this.counterCurrencyCode = counterCurrencyCode.toUpperCase();
        this.pair = this.baseCurrencyCode + "_" + this.counterCurrencyCode;
    }

    public Market(String marketPair) {
        this(marketPair.split("_")[0], marketPair.split("_")[1]);
    }
}
