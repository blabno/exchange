package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;



import bisq.httpapi.facade.PriceFeedFacade;
import bisq.httpapi.model.CurrencyList;
import bisq.httpapi.model.PriceFeed;
import bisq.httpapi.service.ExperimentalFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

@Tag(name = "currencies")
@Produces(MediaType.APPLICATION_JSON)
public class CurrencyEndpoint {

    private final ExperimentalFeature experimentalFeature;
    private final PriceFeedFacade priceFeedFacade;

    @Inject
    public CurrencyEndpoint(ExperimentalFeature experimentalFeature, PriceFeedFacade priceFeedFacade) {
        this.experimentalFeature = experimentalFeature;
        this.priceFeedFacade = priceFeedFacade;
    }

    @Operation(summary = "List available currencies", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =CurrencyList.class))), description = ExperimentalFeature.NOTE)
    @GET
    public void getCurrencyList(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                asyncResponse.resume(MarketEndpoint.getCurrencyList());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Get market prices", description = ExperimentalFeature.NOTE + "If currencyCodes is not provided then currencies from preferences are used.", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =PriceFeed.class))))
    @GET
    @Path("/prices")
    public void getPriceFeed(@Suspended AsyncResponse asyncResponse, @QueryParam("currencyCodes") String currencyCodes) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                String[] codes;
                if (currencyCodes == null || currencyCodes.length() == 0)
                    codes = new String[0];
                else
                    codes = currencyCodes.split("\\s*,\\s*");
                asyncResponse.resume(priceFeedFacade.getPriceFeed(codes));
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
