package bisq.api.http.service.endpoint;

import bisq.api.http.facade.WalletFacade;
import bisq.api.http.model.WalletAddress;
import bisq.api.http.service.ExperimentalFeature;

import bisq.common.UserThread;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;



import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

@Slf4j
@Tag(name = "wallet")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WalletEndpoint {

    private final ExperimentalFeature experimentalFeature;
    private final WalletFacade walletFacade;

    @Inject
    public WalletEndpoint(ExperimentalFeature experimentalFeature, WalletFacade walletFacade) {
        this.experimentalFeature = experimentalFeature;
        this.walletFacade = walletFacade;
    }

    @Operation(summary = "Get or create wallet address", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = WalletAddress.class))), description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.WILDCARD)
    @POST
    @Path("/addresses")
    public void getOrCreateAvailableUnusedWalletAddresses(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                WalletAddress addresses = walletFacade.getOrCreateAvailableUnusedWalletAddresses();
                asyncResponse.resume(addresses);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
