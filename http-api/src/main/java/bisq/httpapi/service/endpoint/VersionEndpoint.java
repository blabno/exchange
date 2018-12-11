package bisq.httpapi.service.endpoint;

import bisq.common.app.Version;



import bisq.httpapi.model.VersionDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Tag(name = "version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionEndpoint {

    @Operation(summary = "Get version details")
    @GET
    public VersionDetails getVersionDetails() {
        final VersionDetails versionDetails = new VersionDetails();
        versionDetails.application = Version.VERSION;
        versionDetails.network = Version.P2P_NETWORK_VERSION;
        versionDetails.p2PMessage = Version.getP2PMessageVersion();
        versionDetails.localDB = Version.LOCAL_DB_VERSION;
        versionDetails.tradeProtocol = Version.TRADE_PROTOCOL_VERSION;
        return versionDetails;
    }
}
