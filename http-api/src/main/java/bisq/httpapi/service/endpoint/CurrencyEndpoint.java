package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;



import bisq.httpapi.facade.PriceFeedFacade;
import bisq.httpapi.model.CurrencyList;
import bisq.httpapi.model.PriceFeed;
import bisq.httpapi.service.ExperimentalFeature;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

@Api(value = "currencies", authorizations = @Authorization(value = "accessToken"))
@Produces(MediaType.APPLICATION_JSON)
public class CurrencyEndpoint {

    private final ExperimentalFeature experimentalFeature;
    private final PriceFeedFacade priceFeedFacade;

    @Inject
    public CurrencyEndpoint(ExperimentalFeature experimentalFeature, PriceFeedFacade priceFeedFacade) {
        this.experimentalFeature = experimentalFeature;
        this.priceFeedFacade = priceFeedFacade;
    }

    @ApiOperation(value = "List available currencies", response = CurrencyList.class, notes = ExperimentalFeature.NOTE)
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

    @ApiOperation(value = "Get market prices", notes = ExperimentalFeature.NOTE + "If currencyCodes is not provided then currencies from preferences are used.", response = PriceFeed.class)
    @GET
    @Path("/prices")
    public void getPriceFeed(@Suspended AsyncResponse asyncResponse, @QueryParam("currencyCodes") String currencyCodes) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                String[] codes;
                if (null == currencyCodes || 0 == currencyCodes.length())
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
