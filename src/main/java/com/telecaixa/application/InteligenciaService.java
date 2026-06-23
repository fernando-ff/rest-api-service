package com.telecaixa.application;

import com.telecaixa.infrastructure.gemini.GeminiRestClient;
import com.telecaixa.infrastructure.gemini.GeminiSpecs.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class InteligenciaService {

    @Inject
    @RestClient
    GeminiRestClient geminiClient;

    @ConfigProperty(name = "gemini.api.key")
    String apiKey;

    private static final String PROMPT_SISTEMA = """
        Você é o interpretador de linguagem natural do sistema ZapAgenda.
        O usuário enviará uma mensagem tentando agendar um horário. 
        Sua única função é extrair a Data, a Hora e o Serviço solicitado.
        
        Você deve responder estritamente um objeto JSON puro, sem formatação markdown (não use ```json ... ```), contendo estes campos:
        {
           "data": "AAAA-MM-DD",
           "hora": "HH:MM:SS",
           "serviço": "NOME_DO_SERVIÇO"
        }
        
        Se faltar alguma informação (como a hora), deixe o campo respectivo como null.
        """;

    public Uni<String> extrairDadosAgendamento(String mensagemUsuario) {
        GeminiRequest request = new GeminiRequest(PROMPT_SISTEMA, mensagemUsuario);

        return geminiClient.gerarConteudo(apiKey, request)
                .onItem().transform(GeminiResponse::getTextoExtraido)
                .onItem().transform(String::trim);
    }
}