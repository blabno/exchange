package bisq.httpapi.service.endpoint;

import bisq.common.UserThread;

import javax.inject.Inject;

import java.nio.file.FileAlreadyExistsException;

import java.io.FileNotFoundException;
import java.io.InputStream;



import bisq.httpapi.exceptions.NotFoundException;
import bisq.httpapi.facade.BackupFacade;
import bisq.httpapi.model.BackupList;
import bisq.httpapi.model.CreatedBackup;
import bisq.httpapi.service.ExperimentalFeature;
import bisq.httpapi.util.ResourceHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;


@Tag(name = "backups")
@Produces(MediaType.APPLICATION_JSON)
public class BackupEndpoint {

    private final BackupFacade backupFacade;
    private final ExperimentalFeature experimentalFeature;

    @Inject
    public BackupEndpoint(BackupFacade backupFacade, ExperimentalFeature experimentalFeature) {
        this.backupFacade = backupFacade;
        this.experimentalFeature = experimentalFeature;
    }

    @Operation(summary = "List backups", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = BackupList.class))), description = ExperimentalFeature.NOTE)
    @GET
    public void getBackupList(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                BackupList backupList = new BackupList(backupFacade.getBackupList());
                asyncResponse.resume(backupList);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Create backup", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = CreatedBackup.class))), description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.WILDCARD)
    @POST
    public void createBackup(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                CreatedBackup backup = new CreatedBackup(backupFacade.createBackup());
                asyncResponse.resume(backup);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Upload backup", requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = UploadedFile.class), encoding = @Encoding(name = "file", contentType = MediaType.APPLICATION_OCTET_STREAM))), description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path("/upload")
    public void uploadBackup(@Suspended AsyncResponse asyncResponse,
                             @FormDataParam("file") InputStream uploadedInputStream,
                             @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                try {
                    backupFacade.uploadBackup(fileDetail.getFileName(), uploadedInputStream);
                    asyncResponse.resume(Response.noContent().build());
                } catch (FileAlreadyExistsException e) {
                    throw new ValidationException(e.getMessage());
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Get backup", description = ExperimentalFeature.NOTE)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @GET
    @Path("/{path}")
    public void getBackup(@Suspended AsyncResponse asyncResponse, @PathParam("path") String fileName) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                try {
                    Response response = Response.ok(backupFacade.getBackup(fileName), MediaType.APPLICATION_OCTET_STREAM_TYPE)
                            .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                            .build();
                    asyncResponse.resume(response);
                } catch (FileNotFoundException e) {
                    Response response = ResourceHelper.toValidationErrorResponse(e, 404).type(MediaType.APPLICATION_JSON).build();
                    asyncResponse.resume(response);
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Restore backup", description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.WILDCARD)
    @POST
    @Path("/{path}/restore")
    public void restoreBackup(@Suspended AsyncResponse asyncResponse, @PathParam("path") String fileName) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                try {
                    backupFacade.requestBackupRestore(fileName);
                    asyncResponse.resume(Response.noContent().build());
                } catch (FileNotFoundException e) {
                    throw new NotFoundException(e.getMessage());
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Operation(summary = "Remove backup", description = ExperimentalFeature.NOTE)
    @Consumes(MediaType.WILDCARD)
    @DELETE
    @Path("/{path}")
    public void removeBackup(@Suspended AsyncResponse asyncResponse, @PathParam("path") String fileName) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                try {
                    if (backupFacade.removeBackup(fileName))
                        asyncResponse.resume(Response.noContent().build());
                    else
                        asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unable to remove file: " + fileName).build());
                } catch (FileNotFoundException e) {
                    asyncResponse.resume(ResourceHelper.toValidationErrorResponse(e, 404).build());
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    public static class UploadedFile {

        @Schema(format = "binary")
        public String file;
    }
}
