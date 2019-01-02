package bisq.httpapi.util;


import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;



import bisq.httpapi.exceptions.NotFoundException;
import bisq.httpapi.service.ValidationErrorMessage;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

@Slf4j
public final class EndpointHelper {

    public static Response.ResponseBuilder toValidationErrorResponse(Throwable cause, int status) {
        String message = cause.getMessage();
        ImmutableList<String> list = message == null ? ImmutableList.of() : ImmutableList.of(message);
        return Response.status(status).entity(new ValidationErrorMessage(list));
    }

    public static boolean handleException(AsyncResponse asyncResponse, Throwable throwable) {
        Throwable cause = throwable.getCause();
        Response.ResponseBuilder responseBuilder;
        String message = cause.getMessage();
        if (cause instanceof NotFoundException) {
            responseBuilder = toValidationErrorResponse(cause, 404);
        } else {
            responseBuilder = Response.status(500);
            if (message != null)
                responseBuilder.entity(new ValidationErrorMessage(ImmutableList.of(message)));
            log.error("Unable to remove offer: throwable={}" + throwable);
        }
        return asyncResponse.resume(responseBuilder.build());
    }
}