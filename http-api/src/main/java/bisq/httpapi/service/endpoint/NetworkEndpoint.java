package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;



import bisq.httpapi.facade.NetworkFacade;
import bisq.httpapi.model.BitcoinNetworkStatus;
import bisq.httpapi.model.P2PNetworkStatus;
import bisq.httpapi.service.ExperimentalFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;


@Tag(name = "network")
@Produces(MediaType.APPLICATION_JSON)
public class NetworkEndpoint {

    private final ExperimentalFeature experimentalFeature;
    private final NetworkFacade networkFacade;

    @Inject
    public NetworkEndpoint(ExperimentalFeature experimentalFeature, NetworkFacade networkFacade) {
        this.experimentalFeature = experimentalFeature;
        this.networkFacade = networkFacade;
    }

    @Operation(summary = "Get Bitcoin network status", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =BitcoinNetworkStatus.class))), description = ExperimentalFeature.NOTE)
    @GET
    @Path("/bitcoin/status")
    public void getBitcoinNetworkStatus(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                asyncResponse.resume(networkFacade.getBitcoinNetworkStatus());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Get P2P network status", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =P2PNetworkStatus.class))), description = ExperimentalFeature.NOTE)
    @GET
    @Path("/p2p/status")
    public void getP2PNetworkStatus(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                asyncResponse.resume(networkFacade.getP2PNetworkStatus());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
