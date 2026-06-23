package com.telecaixa.infrastructure.gemini;

import com.telecaixa.infrastructure.gemini.GeminiSpecs.GeminiRequest;
import com.telecaixa.infrastructure.gemini.GeminiSpecs.GeminiResponse;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "gemini-api")
@Path("/models")
public interface GeminiRestClient {

    @POST
    @Path("/gemini-2.5-flash:generateContent")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<GeminiResponse> gerarConteudo(
            @QueryParam("key") String apiKey, 
            GeminiRequest request
    );
}