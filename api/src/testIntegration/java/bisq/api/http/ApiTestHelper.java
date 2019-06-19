/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.api.http;

import bisq.api.http.model.payment.PaymentAccount;
import bisq.api.http.model.payment.SepaPaymentAccount;

import bisq.core.locale.CountryUtil;
import bisq.core.payment.payload.PaymentMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;



import com.github.javafaker.Faker;

@SuppressWarnings("WeakerAccess")
public final class ApiTestHelper {

    public static void waitForAllServicesToBeReady() throws InterruptedException {
//        TODO it would be nice to expose endpoint that would respond with 200
        // PaymentMethod initializes it's static values after all services get initialized
        int ALL_SERVICES_INITIALIZED_DELAY = 5000;
        Thread.sleep(ALL_SERVICES_INITIALIZED_DELAY);
    }

    public static SepaPaymentAccount randomValidCreateSepaAccountPayload(String tradeCurrency, String countryCode) {
        Faker faker = new Faker();
        SepaPaymentAccount accountToCreate = new SepaPaymentAccount();
        if (countryCode == null)
            countryCode = faker.options().nextElement(CountryUtil.getAllSepaCountries()).code;
        accountToCreate.paymentMethod = PaymentMethod.SEPA_ID;
        accountToCreate.accountName = faker.commerce().productName();
        accountToCreate.bic = faker.finance().bic();
        accountToCreate.iban = faker.finance().iban();
        accountToCreate.holderName = faker.name().fullName();
        accountToCreate.countryCode = countryCode;
        accountToCreate.acceptedCountries = new ArrayList<>(new HashSet<>(Arrays.asList("PL", "GB", countryCode)));
        accountToCreate.selectedTradeCurrency = faker.options().option("PLN", "USD", "EUR", "GBP");
        if (tradeCurrency != null)
            accountToCreate.selectedTradeCurrency = tradeCurrency;
        accountToCreate.tradeCurrencies = Collections.singletonList(accountToCreate.selectedTradeCurrency);
        return accountToCreate;
    }

    public static void randomizeAccountPayload(PaymentAccount accountToCreate) {
        Faker faker = new Faker();
        accountToCreate.accountName = faker.commerce().productName();
        accountToCreate.selectedTradeCurrency = faker.options().option("PLN", "USD", "EUR", "GBP");
        accountToCreate.tradeCurrencies = Collections.singletonList(accountToCreate.selectedTradeCurrency);
    }

    public static SepaPaymentAccount randomValidCreateSepaAccountPayload() {
        return randomValidCreateSepaAccountPayload(null, null);
    }

}
