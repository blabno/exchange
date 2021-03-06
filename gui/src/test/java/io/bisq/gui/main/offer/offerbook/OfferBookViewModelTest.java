package io.bisq.gui.main.offer.offerbook;

import com.natpryce.makeiteasy.Maker;
import io.bisq.common.GlobalSettings;
import io.bisq.common.locale.Country;
import io.bisq.common.locale.CryptoCurrency;
import io.bisq.common.locale.FiatCurrency;
import io.bisq.common.locale.Res;
import io.bisq.core.offer.Offer;
import io.bisq.core.offer.OfferPayload;
import io.bisq.core.offer.OpenOfferManager;
import io.bisq.core.payment.*;
import io.bisq.core.payment.payload.*;
import io.bisq.core.provider.price.MarketPrice;
import io.bisq.core.provider.price.PriceFeedService;
import io.bisq.gui.util.BSFormatter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static io.bisq.common.locale.TradeCurrencyMakers.usd;
import static io.bisq.core.user.PreferenceMakers.empty;
import static io.bisq.gui.main.offer.offerbook.OfferBookListItemMaker.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OfferBook.class, OpenOfferManager.class, PriceFeedService.class})
public class OfferBookViewModelTest {
    private static final Logger log = LoggerFactory.getLogger(OfferBookViewModelTest.class);

    @Before
    public void setUp() {
        GlobalSettings.setDefaultTradeCurrency(usd);
        Res.setBaseCurrencyCode(usd.getCode());
        Res.setBaseCurrencyName(usd.getName());
    }

    @Ignore("PaymentAccountPayload needs to be set (has been changed with PB changes)")
    public void testIsAnyPaymentAccountValidForOffer() {
        Collection<PaymentAccount> paymentAccounts;
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSepaAccount("EUR", "DE", "1212324",
                new ArrayList<>(Arrays.asList("AT", "DE")))));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSEPAPaymentMethod("EUR", "AT", new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));


        // empty paymentAccounts
        paymentAccounts = new ArrayList<>();
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(getSEPAPaymentMethod("EUR", "AT",
                new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));

        // simple cases: same payment methods

        // offer: okpay paymentAccount: okpay - same country, same currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getOKPayAccount("EUR")));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getOKPayPaymentMethod("EUR"), paymentAccounts));

        // offer: ether paymentAccount: ether - same country, same currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getCryptoAccount("ETH")));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getBlockChainsPaymentMethod("ETH"), paymentAccounts));

        // offer: sepa paymentAccount: sepa - same country, same currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSepaAccount("EUR", "AT", "1212324",
                new ArrayList<>(Arrays.asList("AT", "DE")))));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSEPAPaymentMethod("EUR", "AT", new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));

        // offer: nationalBank paymentAccount: nationalBank - same country, same currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getNationalBankAccount("EUR", "AT", "PSK")));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // offer: SameBank paymentAccount: SameBank - same country, same currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSameBankAccount("EUR", "AT", "PSK")));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSameBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // offer: sepa paymentAccount: sepa - diff. country, same currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSepaAccount("EUR", "DE", "1212324", new ArrayList<>(Arrays.asList("AT", "DE")))));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSEPAPaymentMethod("EUR", "AT", new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));


        //////

        // offer: sepa paymentAccount: sepa - same country, same currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSepaAccount("EUR", "AT", "1212324", new ArrayList<>(Arrays.asList("AT", "DE")))));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSEPAPaymentMethod("EUR", "AT", new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));


        // offer: sepa paymentAccount: nationalBank - same country, same currency
        // wrong method
        paymentAccounts = new ArrayList<>(Collections.singletonList(getNationalBankAccount("EUR", "AT", "PSK")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSEPAPaymentMethod("EUR", "AT", new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));

        // wrong currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getNationalBankAccount("USD", "US", "XXX")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // wrong country
        paymentAccounts = new ArrayList<>(Collections.singletonList(getNationalBankAccount("EUR", "FR", "PSK")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // sepa wrong country
        paymentAccounts = new ArrayList<>(Collections.singletonList(getNationalBankAccount("EUR", "CH", "PSK")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSEPAPaymentMethod("EUR", "AT", new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));

        // sepa wrong currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getNationalBankAccount("CHF", "DE", "PSK")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getSEPAPaymentMethod("EUR", "AT", new ArrayList<>(Arrays.asList("AT", "DE")), "PSK"), paymentAccounts));


        // same bank
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSameBankAccount("EUR", "AT", "PSK")));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // not same bank
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSameBankAccount("EUR", "AT", "Raika")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // same bank, wrong country
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSameBankAccount("EUR", "DE", "PSK")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // same bank, wrong currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSameBankAccount("USD", "AT", "PSK")));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // spec. bank
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSpecificBanksAccount("EUR", "AT", "PSK",
                new ArrayList<>(Arrays.asList("PSK", "Raika")))));
        assertTrue(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // spec. bank, missing bank
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSpecificBanksAccount("EUR", "AT", "PSK",
                new ArrayList<>(Collections.singletonList("Raika")))));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // spec. bank, wrong country
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSpecificBanksAccount("EUR", "FR", "PSK",
                new ArrayList<>(Arrays.asList("PSK", "Raika")))));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        // spec. bank, wrong currency
        paymentAccounts = new ArrayList<>(Collections.singletonList(getSpecificBanksAccount("USD", "AT", "PSK",
                new ArrayList<>(Arrays.asList("PSK", "Raika")))));
        assertFalse(PaymentAccountUtil.isAnyPaymentAccountValidForOffer(
                getNationalBankPaymentMethod("EUR", "AT", "PSK"), paymentAccounts));

        //TODO add more tests

    }

    @Test
    public void testMaxCharactersForAmountWithNoOffes() {
        OfferBook offerBook = mock(OfferBook.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, null, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        assertEquals(0, model.maxPlacesForAmount.intValue());
    }

    @Test
    public void testMaxCharactersForAmount() {
        OfferBook offerBook = mock(OfferBook.class);
        OpenOfferManager openOfferManager = mock(OpenOfferManager.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();
        offerBookListItems.addAll(make(btcItem));

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, openOfferManager, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        model.activate();

        assertEquals(6, model.maxPlacesForAmount.intValue());
        offerBookListItems.addAll(make(btcItem.but(with(OfferBookListItemMaker.amount, 2000000000L))));
        assertEquals(7, model.maxPlacesForAmount.intValue());
    }

    @Test
    public void testMaxCharactersForAmountRange() {
        OfferBook offerBook = mock(OfferBook.class);
        OpenOfferManager openOfferManager = mock(OpenOfferManager.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();
        offerBookListItems.addAll(make(btcItemWithRange));

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, openOfferManager, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        model.activate();

        assertEquals(15, model.maxPlacesForAmount.intValue());
        offerBookListItems.addAll(make(btcItemWithRange.but(with(OfferBookListItemMaker.amount, 2000000000L))));
        assertEquals(16, model.maxPlacesForAmount.intValue());
        offerBookListItems.addAll(make(btcItemWithRange.but(with(OfferBookListItemMaker.minAmount, 30000000000L),
                with(OfferBookListItemMaker.amount,30000000000L))));
        assertEquals(19, model.maxPlacesForAmount.intValue());
    }

    @Test
    public void testMaxCharactersForVolumeWithNoOffes() {
        OfferBook offerBook = mock(OfferBook.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, null, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        assertEquals(0, model.maxPlacesForVolume.intValue());
    }

    @Test
    public void testMaxCharactersForVolume() {
        OfferBook offerBook = mock(OfferBook.class);
        OpenOfferManager openOfferManager = mock(OpenOfferManager.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();
        offerBookListItems.addAll(make(btcItem));

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, openOfferManager, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        model.activate();

        assertEquals(8, model.maxPlacesForVolume.intValue());
        offerBookListItems.addAll(make(btcItem.but(with(OfferBookListItemMaker.amount, 2000000000L))));
        assertEquals(10, model.maxPlacesForVolume.intValue());
    }

    @Test
    public void testMaxCharactersForVolumeRange() {
        OfferBook offerBook = mock(OfferBook.class);
        OpenOfferManager openOfferManager = mock(OpenOfferManager.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();
        offerBookListItems.addAll(make(btcItemWithRange));

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, openOfferManager, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        model.activate();

        assertEquals(15, model.maxPlacesForVolume.intValue());
        offerBookListItems.addAll(make(btcItemWithRange.but(with(OfferBookListItemMaker.amount, 2000000000L))));
        assertEquals(17, model.maxPlacesForVolume.intValue());
        offerBookListItems.addAll(make(btcItemWithRange.but(with(OfferBookListItemMaker.minAmount, 30000000000L),
                with(OfferBookListItemMaker.amount,30000000000L))));
        assertEquals(25, model.maxPlacesForVolume.intValue());
    }

    @Test
    public void testMaxCharactersForPriceWithNoOffers() {
        OfferBook offerBook = mock(OfferBook.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, null, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        assertEquals(0, model.maxPlacesForPrice.intValue());
    }

    @Test
    public void testMaxCharactersForPrice() {
        OfferBook offerBook = mock(OfferBook.class);
        OpenOfferManager openOfferManager = mock(OpenOfferManager.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();
        offerBookListItems.addAll(make(btcItem));

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, openOfferManager, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        model.activate();

        assertEquals(7, model.maxPlacesForPrice.intValue());
        offerBookListItems.addAll(make(btcItem.but(with(OfferBookListItemMaker.price, 149558240L)))); //14955.8240
        assertEquals(10, model.maxPlacesForPrice.intValue());
        offerBookListItems.addAll(make(btcItem.but(with(OfferBookListItemMaker.price, 14955824L)))); //1495.58240
        assertEquals(10, model.maxPlacesForPrice.intValue());
    }

    @Test
    public void testMaxCharactersForPriceDistanceWithNoOffers() {
        OfferBook offerBook = mock(OfferBook.class);
        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);

        final OfferBookViewModel model = new OfferBookViewModel(null, null, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());
        assertEquals(0, model.maxPlacesForMarketPriceMargin.intValue());
    }

    @Test
    public void testMaxCharactersForPriceDistance() {
        OfferBook offerBook = mock(OfferBook.class);
        OpenOfferManager openOfferManager = mock(OpenOfferManager.class);
        PriceFeedService priceFeedService = mock(PriceFeedService.class);

        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();
        final Maker<io.bisq.gui.main.offer.offerbook.OfferBookListItem> item = btcItem.but(with(useMarketBasedPrice, true));

        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);
        when(priceFeedService.getMarketPrice(anyString())).thenReturn(null);
        when(priceFeedService.updateCounterProperty()).thenReturn(new SimpleIntegerProperty());

        final io.bisq.gui.main.offer.offerbook.OfferBookListItem item1 = make(item);
        item1.getOffer().setPriceFeedService(priceFeedService);
        final io.bisq.gui.main.offer.offerbook.OfferBookListItem item2 = make(item.but(with(marketPriceMargin, 0.0197)));
        item2.getOffer().setPriceFeedService(priceFeedService);
        final io.bisq.gui.main.offer.offerbook.OfferBookListItem item3 = make(item.but(with(marketPriceMargin, 0.1)));
        item3.getOffer().setPriceFeedService(priceFeedService);
        final io.bisq.gui.main.offer.offerbook.OfferBookListItem item4 = make(item.but(with(marketPriceMargin, -0.1)));
        item4.getOffer().setPriceFeedService(priceFeedService);
        offerBookListItems.addAll(item1, item2);

        final OfferBookViewModel model = new OfferBookViewModel(null, openOfferManager, offerBook, empty, null, priceFeedService,
                null, null, null, null, null,
                new BSFormatter());
        model.activate();

        assertEquals(8, model.maxPlacesForMarketPriceMargin.intValue()); //" (1.97%)"
        offerBookListItems.addAll(item3);
        assertEquals(9, model.maxPlacesForMarketPriceMargin.intValue()); //" (10.00%)"
        offerBookListItems.addAll(item4);
        assertEquals(10, model.maxPlacesForMarketPriceMargin.intValue()); //" (-10.00%)"
    }

    @Test
    public void testGetPrice() {
        OfferBook offerBook = mock(OfferBook.class);
        OpenOfferManager openOfferManager = mock(OpenOfferManager.class);
        PriceFeedService priceFeedService = mock(PriceFeedService.class);

        final ObservableList<OfferBookListItem> offerBookListItems = FXCollections.observableArrayList();
        when(offerBook.getOfferBookListItems()).thenReturn(offerBookListItems);
        when(priceFeedService.getMarketPrice(anyString())).thenReturn(new MarketPrice("USD", 12684.0450, Instant.now().getEpochSecond(), true));

        final OfferBookViewModel model = new OfferBookViewModel(null, openOfferManager, offerBook, empty, null, null,
                null, null, null, null, null,
                new BSFormatter());

        final io.bisq.gui.main.offer.offerbook.OfferBookListItem item = make(btcItem.but(
                with(useMarketBasedPrice, true),
                with(marketPriceMargin, -0.12)));

        final io.bisq.gui.main.offer.offerbook.OfferBookListItem lowItem = make(btcItem.but(
                with(useMarketBasedPrice, true),
                with(marketPriceMargin, 0.01)));

        final io.bisq.gui.main.offer.offerbook.OfferBookListItem fixedItem = make(btcItem);

        item.getOffer().setPriceFeedService(priceFeedService);
        lowItem.getOffer().setPriceFeedService(priceFeedService);
        offerBookListItems.addAll(lowItem, fixedItem);
        model.activate();

        assertEquals("12557.2046 (1.00%)", model.getPrice(lowItem));
        assertEquals("   10.0000        ", model.getPrice(fixedItem));
        offerBookListItems.addAll(item);
        assertEquals("14206.1304 (-12.00%)", model.getPrice(item));
        assertEquals("12557.2046   (1.00%)", model.getPrice(lowItem));


    }

    private PaymentAccount getOKPayAccount(String currencyCode) {
        PaymentAccount paymentAccount = new OKPayAccount();
        paymentAccount.setSelectedTradeCurrency(new FiatCurrency(currencyCode));
        return paymentAccount;
    }

    private PaymentAccount getCryptoAccount(String currencyCode) {
        PaymentAccount paymentAccount = new CryptoCurrencyAccount();
        paymentAccount.addCurrency(new CryptoCurrency(currencyCode, null));
        return paymentAccount;
    }

    private PaymentAccount getSepaAccount(String currencyCode, String countryCode, String bic, ArrayList<String> countryCodes) {
        CountryBasedPaymentAccount paymentAccount = new SepaAccount();
        paymentAccount.setSingleTradeCurrency(new FiatCurrency(currencyCode));
        paymentAccount.setCountry(new Country(countryCode, null, null));
        ((SepaAccountPayload) paymentAccount.getPaymentAccountPayload()).setBic(bic);
        countryCodes.forEach(((SepaAccountPayload) paymentAccount.getPaymentAccountPayload())::addAcceptedCountry);
        return paymentAccount;
    }

    private PaymentAccount getNationalBankAccount(String currencyCode, String countryCode, String bankId) {
        CountryBasedPaymentAccount paymentAccount = new NationalBankAccount();
        paymentAccount.setSingleTradeCurrency(new FiatCurrency(currencyCode));
        paymentAccount.setCountry(new Country(countryCode, null, null));
        ((NationalBankAccountPayload) paymentAccount.getPaymentAccountPayload()).setBankId(bankId);
        return paymentAccount;
    }

    private PaymentAccount getSameBankAccount(String currencyCode, String countryCode, String bankId) {
        SameBankAccount paymentAccount = new SameBankAccount();
        paymentAccount.setSingleTradeCurrency(new FiatCurrency(currencyCode));
        paymentAccount.setCountry(new Country(countryCode, null, null));
        ((SameBankAccountPayload) paymentAccount.getPaymentAccountPayload()).setBankId(bankId);
        return paymentAccount;
    }

    private PaymentAccount getSpecificBanksAccount(String currencyCode, String countryCode, String bankId, ArrayList<String> bankIds) {
        SpecificBanksAccount paymentAccount = new SpecificBanksAccount();
        paymentAccount.setSingleTradeCurrency(new FiatCurrency(currencyCode));
        paymentAccount.setCountry(new Country(countryCode, null, null));
        ((SpecificBanksAccountPayload) paymentAccount.getPaymentAccountPayload()).setBankId(bankId);
        bankIds.forEach(((SpecificBanksAccountPayload) paymentAccount.getPaymentAccountPayload())::addAcceptedBank);
        return paymentAccount;
    }


    private Offer getBlockChainsPaymentMethod(String currencyCode) {
        return getOffer(currencyCode,
                PaymentMethod.BLOCK_CHAINS_ID,
                null,
                null,
                null,
                null);
    }

    private Offer getOKPayPaymentMethod(String currencyCode) {
        return getOffer(currencyCode,
                PaymentMethod.OK_PAY_ID,
                null,
                null,
                null,
                null);
    }

    private Offer getSEPAPaymentMethod(String currencyCode, String countryCode, ArrayList<String> countryCodes, String bankId) {
        return getPaymentMethod(currencyCode,
                PaymentMethod.SEPA_ID,
                countryCode,
                countryCodes,
                bankId,
                null);
    }

    private Offer getNationalBankPaymentMethod(String currencyCode, String countryCode, String bankId) {
        return getPaymentMethod(currencyCode,
                PaymentMethod.NATIONAL_BANK_ID,
                countryCode,
                new ArrayList<>(Collections.singletonList(countryCode)),
                bankId,
                null);
    }

    private Offer getSameBankPaymentMethod(String currencyCode, String countryCode, String bankId) {
        return getPaymentMethod(currencyCode,
                PaymentMethod.SAME_BANK_ID,
                countryCode,
                new ArrayList<>(Collections.singletonList(countryCode)),
                bankId,
                new ArrayList<>(Collections.singletonList(bankId)));
    }

    private Offer getSpecificBanksPaymentMethod(String currencyCode, String countryCode, String bankId, ArrayList<String> bankIds) {
        return getPaymentMethod(currencyCode,
                PaymentMethod.SPECIFIC_BANKS_ID,
                countryCode,
                new ArrayList<>(Collections.singletonList(countryCode)),
                bankId,
                bankIds);
    }

    private Offer getPaymentMethod(String currencyCode, String paymentMethodId, String countryCode, ArrayList<String> countryCodes, String bankId, ArrayList<String> bankIds) {
        return getOffer(currencyCode,
                paymentMethodId,
                countryCode,
                countryCodes,
                bankId,
                bankIds);
    }


    private Offer getOffer(String tradeCurrencyCode, String paymentMethodId, String countryCode, ArrayList<String> acceptedCountryCodes, String bankId, ArrayList<String> acceptedBanks) {
        return new Offer(new OfferPayload(null,
                0,
                null,
                null,
                null,
                0,
                0,
                false,
                0,
                0,
                "BTC",
                tradeCurrencyCode,
                null,
                null,
                paymentMethodId,
                null,
                null,
                countryCode,
                acceptedCountryCodes,
                bankId,
                acceptedBanks,
                null,
                0,
                0,
                0,
                false,
                0,
                0,
                0,
                0,
                false,
                false,
                0,
                0,
                false,
                null,
                null,
                1));
    }
}
