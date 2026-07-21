package com.telecaixa.infrastructure.gemini;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.telecaixa.infrastructure.gemini.GeminiRequest;
import com.telecaixa.infrastructure.gemini.GeminiResponse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "gemini-api")
@Path("/v1beta/models")
public interface GeminiRestClient {

    @POST
    @Path("/{modelName}:generateContent")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<GeminiResponse> gerarConteudo(
            @PathParam("modelName") String modelName,
            @QueryParam("key") String apiKey,
            GeminiRequest request
    );
}