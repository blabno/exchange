package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;



import bisq.httpapi.facade.PreferencesFacade;
import bisq.httpapi.model.Preferences;
import bisq.httpapi.model.PreferencesAvailableValues;
import bisq.httpapi.service.ExperimentalFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

@Tag(name = "preferences")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PreferencesEndpoint {

    private final ExperimentalFeature experimentalFeature;
    private final PreferencesFacade preferencesFacade;

    @Inject
    public PreferencesEndpoint(ExperimentalFeature experimentalFeature, PreferencesFacade preferencesFacade) {
        this.experimentalFeature = experimentalFeature;
        this.preferencesFacade = preferencesFacade;
    }

    @Operation(summary = "Get preferences", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =Preferences.class))), description = ExperimentalFeature.NOTE)
    @GET
    public void getPreferences(@Suspended final AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                asyncResponse.resume(preferencesFacade.getPreferences());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Set preferences", description = ExperimentalFeature.NOTE + "\nSupports partial update", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =Preferences.class))))
    @PUT
    public void setPreferences(@Suspended final AsyncResponse asyncResponse, @Valid Preferences preferences) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                asyncResponse.resume(preferencesFacade.setPreferences(preferences));
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Get available preferences values", responses =  @ApiResponse(content = @Content(schema = @Schema(implementation =PreferencesAvailableValues.class))), description = ExperimentalFeature.NOTE)
    @GET
    @Path("/available-values")
    public void getPreferencesAvailableValues(@Suspended final AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                asyncResponse.resume(preferencesFacade.getPreferencesAvailableValues());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
