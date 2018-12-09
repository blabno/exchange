package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;



import bisq.httpapi.facade.ClosedTradableFacade;
import bisq.httpapi.model.ClosedTradableList;
import bisq.httpapi.service.ExperimentalFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

@Slf4j
@Tag(name = "closed-tradables")
@Produces(MediaType.APPLICATION_JSON)
public class ClosedTradableEndpoint {

    private final ClosedTradableFacade closedTradableFacade;
    private final ExperimentalFeature experimentalFeature;

    @Inject
    public ClosedTradableEndpoint(ClosedTradableFacade closedTradableFacade, ExperimentalFeature experimentalFeature) {
        this.closedTradableFacade = closedTradableFacade;
        this.experimentalFeature = experimentalFeature;
    }

    @Operation(summary = "List portfolio history", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =ClosedTradableList.class))), description = ExperimentalFeature.NOTE)
    @GET
    public void listClosedTrades(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                ClosedTradableList list = new ClosedTradableList();
                list.closedTradables = closedTradableFacade.getClosedTradableList();
                list.total = list.closedTradables.size();
                asyncResponse.resume(list);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

}
