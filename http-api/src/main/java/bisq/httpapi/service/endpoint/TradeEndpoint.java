package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

import static bisq.httpapi.util.ResourceHelper.toValidationErrorResponse;
import static java.util.stream.Collectors.toList;



import bisq.httpapi.exceptions.NotFoundException;
import bisq.httpapi.facade.TradeFacade;
import bisq.httpapi.model.TradeDetails;
import bisq.httpapi.model.TradeList;
import bisq.httpapi.service.ValidationErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.ValidationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.hibernate.validator.constraints.NotEmpty;

@Slf4j
@Tag(name = "trades")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TradeEndpoint {

    private final TradeFacade tradeFacade;

    @Inject
    public TradeEndpoint(TradeFacade tradeFacade) {
        this.tradeFacade = tradeFacade;
    }

    @Operation(summary = "List trades", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =TradeList.class))))
    @GET
    public void find(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                TradeList tradeList = new TradeList();
                tradeList.trades = tradeFacade.getTradeList().stream().map(TradeDetails::new).collect(toList());
                tradeList.total = tradeList.trades.size();
                asyncResponse.resume(tradeList);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Get trade details", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =TradeDetails.class))))
    @GET
    @Path("/{id}")
    public void getById(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id) {
        UserThread.execute(() -> {
            try {
                asyncResponse.resume(new TradeDetails(tradeFacade.getTrade(id)));
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary="Confirm payment has started")
    @Consumes(MediaType.WILDCARD)
    @POST
    @Path("/{id}/payment-started")
    public void paymentStarted(@Suspended AsyncResponse asyncResponse, @NotEmpty @PathParam("id") String id) {
        UserThread.execute(() -> {
            try {
                CompletableFuture<Void> completableFuture = tradeFacade.paymentStarted(id);
                handlePaymentStatusChange(id, asyncResponse, completableFuture);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Confirm payment has been received")
    @Consumes(MediaType.WILDCARD)
    @POST
    @Path("/{id}/payment-received")
    public void paymentReceived(@Suspended AsyncResponse asyncResponse, @NotEmpty @PathParam("id") String id) {
        UserThread.execute(() -> {
            try {
                CompletableFuture<Void> completableFuture = tradeFacade.paymentReceived(id);
                handlePaymentStatusChange(id, asyncResponse, completableFuture);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Move funds to Bisq wallet")
    @Consumes(MediaType.WILDCARD)
    @POST
    @Path("/{id}/move-funds-to-bisq-wallet")
    public void moveFundsToBisqWallet(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id) {
        UserThread.execute(() -> {
            try {
                tradeFacade.moveFundsToBisqWallet(id);
                asyncResponse.resume(Response.status(Response.Status.OK).build());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    private void handlePaymentStatusChange(String tradeId, AsyncResponse asyncResponse, CompletableFuture<Void> completableFuture) {
        completableFuture.thenApply(response -> asyncResponse.resume(Response.status(Response.Status.OK).build()))
                .exceptionally(e -> {
                    Throwable cause = e.getCause();
                    Response.ResponseBuilder responseBuilder;
                    if (cause instanceof ValidationException) {
                        responseBuilder = toValidationErrorResponse(cause, 422);
                    } else if (cause instanceof NotFoundException) {
                        responseBuilder = toValidationErrorResponse(cause, 404);
                    } else {
                        String message = cause.getMessage();
                        responseBuilder = Response.status(500);
                        if (message != null)
                            responseBuilder.entity(new ValidationErrorMessage(ImmutableList.of(message)));
                        log.error("Unable to confirm payment started for trade: " + tradeId, cause);
                    }
                    return asyncResponse.resume(responseBuilder.build());
                });
    }

}
