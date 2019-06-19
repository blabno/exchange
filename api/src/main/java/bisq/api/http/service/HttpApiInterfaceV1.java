package bisq.api.http.service;

import bisq.api.http.service.endpoint.ArbitratorEndpoint;
import bisq.api.http.service.endpoint.OfferEndpoint;
import bisq.api.http.service.endpoint.PaymentAccountEndpoint;
import bisq.api.http.service.endpoint.UserEndpoint;
import bisq.api.http.service.endpoint.VersionEndpoint;
import bisq.api.http.service.endpoint.WalletEndpoint;

import javax.inject.Inject;



import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Path;

@SecurityScheme(
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        name = "authorization",
        paramName = "authorization"
)
@OpenAPIDefinition(
        info = @Info(version = "0.0.1", title = "Bisq HTTP API"),
        security = @SecurityRequirement(name = "authorization"),
        tags = {
                @Tag(name = "arbitrators"),
                @Tag(name = "offers"),
                @Tag(name = "payment-accounts"),
                @Tag(name = "user"),
                @Tag(name = "version"),
                @Tag(name = "wallet")
        }
)
@Path("/api/v1")
public class HttpApiInterfaceV1 {
    private final ArbitratorEndpoint arbitratorEndpoint;
    private final PaymentAccountEndpoint paymentAccountEndpoint;
    private final OfferEndpoint offerEndpoint;
    private final UserEndpoint userEndpoint;
    private final VersionEndpoint versionEndpoint;
    private final WalletEndpoint walletEndpoint;

    @Inject
    public HttpApiInterfaceV1(ArbitratorEndpoint arbitratorEndpoint, PaymentAccountEndpoint paymentAccountEndpoint, OfferEndpoint offerEndpoint, UserEndpoint userEndpoint, VersionEndpoint versionEndpoint, WalletEndpoint walletEndpoint) {
        this.arbitratorEndpoint = arbitratorEndpoint;
        this.paymentAccountEndpoint = paymentAccountEndpoint;
        this.offerEndpoint = offerEndpoint;
        this.userEndpoint = userEndpoint;
        this.versionEndpoint = versionEndpoint;
        this.walletEndpoint = walletEndpoint;
    }

    @Path("arbitrators")
    public ArbitratorEndpoint getArbitratorEndpoint() {
        return arbitratorEndpoint;
    }

    @Path("offers")
    public OfferEndpoint getOfferEndpoint() {
        return offerEndpoint;
    }

    @Path("payment-accounts")
    public PaymentAccountEndpoint getPaymentAccountEndpoint() {
        return paymentAccountEndpoint;
    }

    @Path("user")
    public UserEndpoint getUserEndpoint() {
        return userEndpoint;
    }

    @Path("version")
    public VersionEndpoint getVersionEndpoint() {
        return versionEndpoint;
    }

    @Path("wallet")
    public WalletEndpoint getWalletEndpoint() {
        return walletEndpoint;
    }
}
