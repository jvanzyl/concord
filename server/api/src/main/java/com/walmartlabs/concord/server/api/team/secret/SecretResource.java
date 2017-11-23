package com.walmartlabs.concord.server.api.team.secret;

import com.walmartlabs.concord.common.validation.ConcordKey;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api("Secrets")
@Path("/api/v1/team")
public interface SecretResource {

    @POST
    @ApiOperation("Creates a new secret")
    @Path("/{teamName}/secret")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    SecretOperationResponse create(@ApiParam @PathParam("teamName") @ConcordKey String teamName,
                                   @ApiParam MultipartInput input);

    @GET
    @ApiOperation("Retrieves the public key of a key pair")
    @Path("/{teamName}/secret/{secretName}/public")
    @Produces(MediaType.APPLICATION_JSON)
    PublicKeyResponse getPublicKey(@ApiParam @PathParam("teamName") @ConcordKey String teamName,
                                   @ApiParam @PathParam("secretName") @ConcordKey String secretName);

    @GET
    @ApiOperation("List secrets")
    @Path("/{teamName}/secret")
    @Produces(MediaType.APPLICATION_JSON)
    List<SecretEntry> list(@ApiParam @PathParam("teamName") @ConcordKey String teamName);

    @DELETE
    @ApiOperation("Delete an existing secret")
    @Path("/{teamName}/secret/{secretName}")
    @Produces(MediaType.APPLICATION_JSON)
    DeleteSecretResponse delete(@ApiParam @PathParam("teamName") @ConcordKey String teamName,
                                @ApiParam @PathParam("secretName") @ConcordKey String secretName);
}