package com.telecaixa.infrastructure.gemini;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.telecaixa.infrastructure.gemini.GeminiSpecs.GeminiRequest;
import com.telecaixa.infrastructure.gemini.GeminiSpecs.GeminiResponse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "gemini-api")
@Path("/models")
public interface GeminiRestClient {

    @POST
    @Path("/gemini-1.5-flash:generateContent")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<GeminiResponse> gerarConteudo(
            @QueryParam("key") String apiKey, 
            GeminiRequest request
    );
}