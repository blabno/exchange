package bisq.httpapi.facade;

import bisq.core.locale.CryptoCurrency;
import bisq.core.locale.FiatCurrency;
import bisq.core.locale.TradeCurrency;
import bisq.core.provider.price.MarketPrice;
import bisq.core.provider.price.PriceFeedService;
import bisq.core.user.Preferences;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;



import bisq.httpapi.model.PriceFeed;

public class PriceFeedFacade {

    private final Preferences preferences;
    private final PriceFeedService priceFeedService;

    @Inject
    public PriceFeedFacade(bisq.core.user.Preferences preferences,
                           PriceFeedService priceFeedService) {
        this.preferences = preferences;
        this.priceFeedService = priceFeedService;
    }

    public PriceFeed getPriceFeed(String[] codes) {
        List<FiatCurrency> fiatCurrencies = preferences.getFiatCurrencies();
        List<CryptoCurrency> cryptoCurrencies = preferences.getCryptoCurrencies();
        Stream<String> codesStream;
        if (codes == null || codes.length == 0)
            codesStream = Stream.concat(fiatCurrencies.stream(), cryptoCurrencies.stream()).map(TradeCurrency::getCode);
        else
            codesStream = Arrays.stream(codes);
        List<MarketPrice> marketPrices = codesStream
                .map(priceFeedService::getMarketPrice)
                .filter(Objects::nonNull)
                .collect(toList());
        PriceFeed priceFeed = new PriceFeed();
        for (MarketPrice price : marketPrices)
            priceFeed.prices.put(price.getCurrencyCode(), price.getPrice());
        return priceFeed;
    }
}
