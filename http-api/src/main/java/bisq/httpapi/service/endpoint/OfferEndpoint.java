package bisq.httpapi.service.endpoint;

import bisq.core.offer.Offer;
import bisq.core.trade.Trade;

import bisq.common.UserThread;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

import static bisq.httpapi.util.ResourceHelper.toValidationErrorResponse;



import bisq.httpapi.exceptions.AmountTooHighException;
import bisq.httpapi.exceptions.IncompatiblePaymentAccountException;
import bisq.httpapi.exceptions.InsufficientMoneyException;
import bisq.httpapi.exceptions.NoAcceptedArbitratorException;
import bisq.httpapi.exceptions.NotFoundException;
import bisq.httpapi.exceptions.PaymentAccountNotFoundException;
import bisq.httpapi.facade.OfferFacade;
import bisq.httpapi.model.InputDataForOffer;
import bisq.httpapi.model.OfferDetail;
import bisq.httpapi.model.OfferList;
import bisq.httpapi.model.TakeOffer;
import bisq.httpapi.model.TradeDetails;
import bisq.httpapi.service.ValidationErrorMessage;
import bisq.httpapi.util.ResourceHelper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
@Tag(name = "offers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OfferEndpoint {

    private final OfferFacade offerFacade;


    @Inject
    public OfferEndpoint(OfferFacade offerFacade) {
        this.offerFacade = offerFacade;
    }


    @Operation(summary = "Find offers", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = OfferList.class))))
    @GET
    public void find(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                List<OfferDetail> offers = offerFacade.getAllOffers();
                asyncResponse.resume(new OfferList(offers));
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Get offer details", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = OfferDetail.class))))
    @GET
    @Path("/{id}")
    public void getOfferById(@Suspended AsyncResponse asyncResponse, @NotEmpty @PathParam("id") String id) {
        UserThread.execute(() -> {
            try {
                Offer offer = offerFacade.findOffer(id);
                asyncResponse.resume(new OfferDetail(offer));
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Cancel offer")
    @DELETE
    @Path("/{id}")
    public void cancelOffer(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id) {
        CompletableFuture<Void> completableFuture = offerFacade.cancelOffer(id);
        completableFuture.thenApply(response -> asyncResponse.resume(Response.status(200).build()))
                .exceptionally(throwable -> ResourceHelper.handleException(asyncResponse, throwable));
    }

    @Operation(summary = "Create offer", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = OfferDetail.class))))
    @POST
    public void createOffer(@Suspended AsyncResponse asyncResponse, @Valid InputDataForOffer input) {
        CompletableFuture<Offer> completableFuture = offerFacade.createOffer(input);
        completableFuture.thenApply(response -> asyncResponse.resume(new OfferDetail(response)))
                .exceptionally(e -> {
                    Throwable cause = e.getCause();
                    Response.ResponseBuilder responseBuilder;
                    if (cause instanceof ValidationException) {
                        responseBuilder = toValidationErrorResponse(cause, 422);
                    } else if (cause instanceof IncompatiblePaymentAccountException) {
                        responseBuilder = toValidationErrorResponse(cause, 423);
                    } else if (cause instanceof NoAcceptedArbitratorException) {
                        responseBuilder = toValidationErrorResponse(cause, 424);
                    } else if (cause instanceof PaymentAccountNotFoundException) {
                        responseBuilder = toValidationErrorResponse(cause, 425);
                    } else if (cause instanceof AmountTooHighException) {
                        responseBuilder = toValidationErrorResponse(cause, 426);
                    } else if (cause instanceof InsufficientMoneyException) {
                        responseBuilder = toValidationErrorResponse(cause, 427);
                    } else {
                        String message = cause.getMessage();
                        responseBuilder = Response.status(500);
                        if (null != message)
                            responseBuilder.entity(new ValidationErrorMessage(ImmutableList.of(message)));
                        log.error("Unable to create offer: " + Json.pretty(input), cause);
                    }
                    return asyncResponse.resume(responseBuilder.build());
                });
    }

    @Operation(summary = "Take offer", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = TradeDetails.class))))
    @POST
    @Path("/{id}/take")
    public void takeOffer(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id, @Valid TakeOffer data) {
        UserThread.execute(() -> {
            try {
                CompletableFuture<Trade> completableFuture = offerFacade.offerTake(id, data.paymentAccountId, data.amount, true, data.maxFundsForTrade);
                completableFuture.thenApply(trade -> asyncResponse.resume(new TradeDetails(trade)))
                        .exceptionally(e -> {
                            Throwable cause = e.getCause();
                            Response.ResponseBuilder responseBuilder;
                            if (cause instanceof ValidationException) {
                                int status = 422;
                                responseBuilder = toValidationErrorResponse(cause, status);
                            } else if (cause instanceof NotFoundException) {
                                responseBuilder = toValidationErrorResponse(cause, 404);
                            } else {
                                String message = cause.getMessage();
                                responseBuilder = Response.status(500);
                                if (null != message)
                                    responseBuilder.entity(new ValidationErrorMessage(ImmutableList.of(message)));
                                log.error("Unable to take offer: " + id + " " + Json.pretty(data), cause);
                            }
                            return asyncResponse.resume(responseBuilder.build());
                        });
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
