package bisq.api.http.facade;

import bisq.api.http.model.Market;

import bisq.core.locale.CurrencyUtil;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class MarketFacade {

    public static List<Market> getMarketList() {
        List<Market> list = CurrencyUtil.getAllSortedCryptoCurrencies().stream()
                .filter(cryptoCurrency -> !(cryptoCurrency.getCode().equals("BTC")))
                .map(cryptoCurrency -> new Market(cryptoCurrency.getCode(), "BTC"))
                .collect(toList());
        list.addAll(CurrencyUtil.getAllSortedFiatCurrencies().stream()
                .map(cryptoCurrency -> new Market("BTC", cryptoCurrency.getCode()))
                .collect(toList()));
        return list;
    }

}
