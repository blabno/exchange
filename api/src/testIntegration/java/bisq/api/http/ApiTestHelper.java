package bisq.api.http;

import bisq.api.http.model.ArbitratorList;
import bisq.api.http.model.OfferDetail;
import bisq.api.http.model.payment.PaymentAccount;
import bisq.api.http.model.payment.SepaPaymentAccount;

import bisq.core.locale.CountryUtil;
import bisq.core.payment.payload.PaymentMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;



import com.github.javafaker.Faker;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.spi.CubeOutput;

@SuppressWarnings("WeakerAccess")
public final class ApiTestHelper {

    public static ValidatableResponse createPaymentAccount(int apiPort, PaymentAccount accountToCreate) {
        return given().
                port(apiPort).
                contentType(ContentType.JSON).
                body(accountToCreate).
//
        when().
                        post("/api/v1/payment-accounts").
//
        then().
                        statusCode(200);
    }

    public static List<String> getAcceptedArbitrators(int apiPort) {
        return given().
                port(apiPort).
                queryParam("acceptedOnly", "true").
//
        when().
                        get("/api/v1/arbitrators").
//
        then().
                        extract().as(ArbitratorList.class).
                        arbitrators.
                        stream().
                        map(arbitrator -> arbitrator.address).
                        collect(Collectors.toList());
    }

    public static void registerArbitrator(int apiPort) throws InterruptedException {
        given().
                port(apiPort).
//
        when().
                body("{\"languageCodes\":[\"en\",\"de\"]}").
                contentType(ContentType.JSON).
                post("/api/v1/arbitrators").
//
        then().
                statusCode(204);

        /* Wait for arbiter registration message to be broadcast across peers*/
        waitForP2PMsgPropagation();
    }

    public static void waitForP2PMsgPropagation() throws InterruptedException {
        int P2P_MSG_RELAY_DELAY = 1000;
        Thread.sleep(P2P_MSG_RELAY_DELAY);
    }

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

    public static void generateBlocks(Container bitcoin, int numberOfBlocks) {
        CubeOutput cubeOutput = bitcoin.exec("bitcoin-cli", "-regtest", "generate", "" + numberOfBlocks);
        assertEquals("Command 'generate blocks' should succeed", "", cubeOutput.getError());
        int ALL_SERVICES_INITIALIZED_DELAY = 3000;
        try {
            Thread.sleep(ALL_SERVICES_INITIALIZED_DELAY);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAvailableBtcWalletAddress(int apiPort) {
        return given().
                port(apiPort).
//
        when().
                        post("/api/v1/wallet/addresses").
//
        then().
                        statusCode(200)
                .extract().body().jsonPath().getString("address");
    }

    public static void sendFunds(Container bitcoin, String walletAddress, double amount) {
        CubeOutput cubeOutput = bitcoin.exec("bitcoin-cli", "-regtest", "sendtoaddress", walletAddress, "" + amount);
        assertEquals("Command 'sendfrom' should succeed", "", cubeOutput.getError());
    }

    public static String[] toString(Enum[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].name();
        }
        return result;
    }

    public static void deselectAllArbitrators(int apiPort) {
        getArbitrators(apiPort).forEach(arbitratorAddress -> deselectArbitrator(apiPort, arbitratorAddress));
    }

    private static void deselectArbitrator(int apiPort, String arbitratorAddress) {
        given().
                port(apiPort).
//
        when().
                post("/api/v1/arbitrators/" + arbitratorAddress + "/deselect").
//
        then().
                statusCode(200)
        ;
    }

    public static void selectArbitrator(int apiPort, String arbitratorAddress) {
        given().
                port(apiPort).
//
        when().
                post("/api/v1/arbitrators/" + arbitratorAddress + "/select").
//
        then().
                statusCode(200)
        ;
    }

    public static List<String> getArbitrators(int apiPort) {
        return given().
                port(apiPort).
//
        when().
                        get("/api/v1/arbitrators").
//
        then().
                        extract().as(ArbitratorList.class).
                        arbitrators.
                        stream().
                        map(arbitrator -> arbitrator.address).
                        collect(Collectors.toList());
    }

    public static OfferDetail getOfferById(int apiPort, String offerId) {
        return given().
                port(apiPort).
//
        when().
                        get("/api/v1/offers/" + offerId).
//
        then().
                        statusCode(200).
                        extract().as(OfferDetail.class)
                ;
    }
}
