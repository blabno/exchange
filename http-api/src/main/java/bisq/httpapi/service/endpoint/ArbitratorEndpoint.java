package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;

import java.util.Collection;
import java.util.stream.Collectors;



import bisq.httpapi.facade.ArbitratorFacade;
import bisq.httpapi.model.Arbitrator;
import bisq.httpapi.model.ArbitratorList;
import bisq.httpapi.model.ArbitratorRegistration;
import bisq.httpapi.service.ExperimentalFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.hibernate.validator.constraints.NotBlank;

@Tag(name = "arbitrators")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ArbitratorEndpoint {

    private final ArbitratorFacade arbitratorFacade;
    private final ExperimentalFeature experimentalFeature;

    @Inject
    public ArbitratorEndpoint(ArbitratorFacade arbitratorFacade, ExperimentalFeature experimentalFeature) {
        this.arbitratorFacade = arbitratorFacade;
        this.experimentalFeature = experimentalFeature;
    }

    private static ArbitratorList toRestModel(Collection<bisq.core.arbitration.Arbitrator> businessModelList) {
        ArbitratorList arbitratorList = new ArbitratorList();
        arbitratorList.arbitrators = businessModelList
                .stream()
                .map(arbitrator -> new Arbitrator(arbitrator.getNodeAddress().getFullAddress()))
                .collect(Collectors.toList());
        arbitratorList.total = arbitratorList.arbitrators.size();
        return arbitratorList;
    }

    @Operation(summary = "Unregister yourself as arbitrator", description = ExperimentalFeature.NOTE)
    @DELETE
    public void unregister() {
        throw new WebApplicationException(Response.Status.NOT_IMPLEMENTED);
    }

    @Operation(summary = "Register yourself as arbitrator", description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public void register(@Suspended AsyncResponse asyncResponse, @Valid @NotNull ArbitratorRegistration data) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                arbitratorFacade.registerArbitrator(data.languageCodes).get();
                asyncResponse.resume(Response.noContent().build());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Find available arbitrators", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = ArbitratorList.class))), description = ExperimentalFeature.NOTE)
    @GET
    public void find(@Suspended AsyncResponse asyncResponse, @QueryParam("acceptedOnly") boolean acceptedOnly) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                ArbitratorList arbitratorList = toRestModel(arbitratorFacade.getArbitrators(acceptedOnly));
                asyncResponse.resume(arbitratorList);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Select arbitrator", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = ArbitratorList.class))), description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.WILDCARD)
    @POST
    @Path("/{address}/select")
    public void selectArbitrator(@Suspended AsyncResponse asyncResponse, @NotBlank @PathParam("address") String address) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                ArbitratorList arbitratorList = toRestModel(arbitratorFacade.selectArbitrator(address));
                asyncResponse.resume(arbitratorList);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Deselect arbitrator", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = ArbitratorList.class))), description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.WILDCARD)
    @POST
    @Path("/{address}/deselect")
    public void deselectArbitrator(@Suspended AsyncResponse asyncResponse, @NotBlank @PathParam("address") String address) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                ArbitratorList arbitratorList = toRestModel(arbitratorFacade.deselectArbitrator(address));
                asyncResponse.resume(arbitratorList);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
