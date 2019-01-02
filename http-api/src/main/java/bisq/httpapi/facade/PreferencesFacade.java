package bisq.httpapi.facade;

import bisq.core.app.BisqEnvironment;
import bisq.core.locale.Country;
import bisq.core.locale.CountryUtil;
import bisq.core.locale.CryptoCurrency;
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.FiatCurrency;
import bisq.core.locale.TradeCurrency;
import bisq.core.user.BlockChainExplorer;

import javax.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



import bisq.httpapi.model.Preferences;
import bisq.httpapi.model.PreferencesAvailableValues;
import javax.validation.ValidationException;

public class PreferencesFacade {

    private final bisq.core.user.Preferences preferences;

    @Inject
    public PreferencesFacade(bisq.core.user.Preferences preferences) {
        this.preferences = preferences;
    }

    public Preferences getPreferences() {
        Preferences preferences = new Preferences();
        preferences.autoSelectArbitrators = this.preferences.isAutoSelectArbitrators();
        preferences.baseCurrencyNetwork = BisqEnvironment.getBaseCurrencyNetwork().getCurrencyCode();
        preferences.blockChainExplorer = this.preferences.getBlockChainExplorer().name;
        preferences.cryptoCurrencies = tradeCurrenciesToCodes(this.preferences.getCryptoCurrencies());
        preferences.fiatCurrencies = tradeCurrenciesToCodes(this.preferences.getFiatCurrencies());
        preferences.ignoredTraders = this.preferences.getIgnoreTradersList();
        preferences.maxPriceDistance = this.preferences.getMaxPriceDistanceInPercent();
        preferences.preferredTradeCurrency = this.preferences.getPreferredTradeCurrency().getCode();
        preferences.useCustomWithdrawalTxFee = this.preferences.getUseCustomWithdrawalTxFeeProperty().get();
        Country userCountry = this.preferences.getUserCountry();
        if (userCountry != null)
            preferences.userCountry = userCountry.code;
        preferences.userLanguage = this.preferences.getUserLanguage();
        preferences.withdrawalTxFee = this.preferences.getWithdrawalTxFeeInBytes();
        return preferences;
    }

    public PreferencesAvailableValues getPreferencesAvailableValues() {
        PreferencesAvailableValues availableValues = new PreferencesAvailableValues();
        availableValues.blockChainExplorers = preferences.getBlockChainExplorers().stream().map(i -> i.name).collect(Collectors.toList());
        availableValues.cryptoCurrencies = tradeCurrenciesToCodes(CurrencyUtil.getAllSortedCryptoCurrencies());
        availableValues.fiatCurrencies = tradeCurrenciesToCodes(CurrencyUtil.getAllSortedFiatCurrencies());
        availableValues.userCountries = CountryUtil.getAllCountries().stream().map(i -> i.code).collect(Collectors.toList());
        return availableValues;
    }

    public Preferences setPreferences(Preferences update) {
        if (update.autoSelectArbitrators != null) {
            preferences.setAutoSelectArbitrators(update.autoSelectArbitrators);
        }
        if (update.baseCurrencyNetwork != null && !update.baseCurrencyNetwork.equals(BisqEnvironment.getBaseCurrencyNetwork().getCurrencyCode())) {
            throw new ValidationException("Changing baseCurrencyNetwork is not supported");
        }
        if (update.blockChainExplorer != null) {
            Optional<BlockChainExplorer> explorerOptional = preferences.getBlockChainExplorers().stream().filter(i -> update.blockChainExplorer.equals(i.name)).findAny();
            if (!explorerOptional.isPresent()) {
                throw new ValidationException("Unsupported value of blockChainExplorer: " + update.blockChainExplorer);
            }
            preferences.setBlockChainExplorer(explorerOptional.get());
        }
        if (update.cryptoCurrencies != null) {
            List<CryptoCurrency> cryptoCurrencies = preferences.getCryptoCurrencies();
            Collection<CryptoCurrency> convertedCryptos = codesToCryptoCurrencies(update.cryptoCurrencies);
            cryptoCurrencies.clear();
            cryptoCurrencies.addAll(convertedCryptos);
        }
        if (update.fiatCurrencies != null) {
            List<FiatCurrency> fiatCurrencies = preferences.getFiatCurrencies();
            Collection<FiatCurrency> convertedFiat = codesToFiatCurrencies(update.fiatCurrencies);
            fiatCurrencies.clear();
            fiatCurrencies.addAll(convertedFiat);
        }
        if (update.ignoredTraders != null) {
            preferences.setIgnoreTradersList(update.ignoredTraders.stream().map(i -> i.replace(":9999", "").replace(".onion", "")).collect(Collectors.toList()));
        }
        if (update.maxPriceDistance != null) {
            preferences.setMaxPriceDistanceInPercent(update.maxPriceDistance);
        }
        if (update.preferredTradeCurrency != null) {
            preferences.setPreferredTradeCurrency(codeToTradeCurrency(update.preferredTradeCurrency));
        }
        if (update.useCustomWithdrawalTxFee != null) {
            preferences.setUseCustomWithdrawalTxFee(update.useCustomWithdrawalTxFee);
        }
        if (update.userCountry != null) {
            preferences.setUserCountry(codeToCountry(update.userCountry));
        }
        if (update.userLanguage != null) {
            preferences.setUserLanguage(update.userLanguage);
        }
        if (update.withdrawalTxFee != null) {
            preferences.setWithdrawalTxFeeInBytes(update.withdrawalTxFee);
        }
        return getPreferences();
    }

    private static Country codeToCountry(String code) {
        Optional<Country> countryOptional = CountryUtil.findCountryByCode(code);
        if (!countryOptional.isPresent())
            throw new ValidationException("Unsupported country code: " + code);
        return countryOptional.get();
    }

    private Collection<CryptoCurrency> codesToCryptoCurrencies(List<String> cryptoCurrencies) {
        return cryptoCurrencies.stream().map(code -> {
            Optional<CryptoCurrency> cryptoCurrency = CurrencyUtil.getCryptoCurrency(code);
            if (!cryptoCurrency.isPresent())
                throw new ValidationException("Unsupported crypto currency code: " + code);
            return cryptoCurrency.get();
        }).collect(Collectors.toList());
    }

    private Collection<FiatCurrency> codesToFiatCurrencies(List<String> fiatCurrencies) {
        return fiatCurrencies.stream().map(code -> {
            Optional<FiatCurrency> cryptoCurrency = CurrencyUtil.getFiatCurrency(code);
            if (!cryptoCurrency.isPresent())
                throw new ValidationException("Unsupported fiat currency code: " + code);
            return cryptoCurrency.get();
        }).collect(Collectors.toList());
    }

    private static TradeCurrency codeToTradeCurrency(String code) {
        Optional<TradeCurrency> currencyOptional = CurrencyUtil.getTradeCurrency(code);
        if (!currencyOptional.isPresent())
            throw new ValidationException("Unsupported trade currency code: " + code);
        return currencyOptional.get();
    }

    private static List<String> tradeCurrenciesToCodes(Collection<? extends TradeCurrency> tradeCurrencies) {
        return tradeCurrencies.stream().map(TradeCurrency::getCode).collect(Collectors.toList());
    }
}