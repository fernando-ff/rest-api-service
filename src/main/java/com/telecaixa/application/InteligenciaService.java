package com.telecaixa.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecaixa.infrastructure.gemini.GeminiRequest;
import com.telecaixa.infrastructure.gemini.GeminiRestClient;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.time.LocalDate;

@ApplicationScoped
public class InteligenciaService {

    @Inject
    @RestClient
    GeminiRestClient geminiClient; // Mantido o nome injetado

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "gemini.api.key")
    String apiKey;

    public Uni<String> extrairDadosAgendamento(String mensagemUsuario) {
        // Injetamos o ano corrente dinamicamente no prompt para garantir parsing exato de datas
        String promptSistema = """
            Você é o interpretador de linguagem natural do sistema ZapAgenda.
            O usuário enviará uma mensagem tentando agendar um horário. 
            Sua única função é extrair a Data, a Hora e o Serviço solicitado.
            Considere que o ano atual é %d.
            
            Você deve responder EXCLUSIVAMENTE um objeto JSON puro, sem formatação markdown (NÃO use marcadores ```json ou ```), contendo estes campos:
            {
               "data": "AAAA-MM-DD",
               "hora": "HH:MM:SS",
               "servico": "NOME_DO_SERVIÇO"
            }
            
            Se faltar alguma informação, preencha o campo correspondente com null.
            """.formatted(LocalDate.now().getYear());

        GeminiRequest request = new GeminiRequest(promptSistema, mensagemUsuario);

        // Fallback genérico caso todas as tentativas contra a API do Gemini falhem
        String fallbackJson = """
            {"data": null, "hora": null, "servico": null}
            """;

        // Log request payload to help debug native deserialization / API behavior
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            Log.infof("→ Gemini Request JSON: %s", requestJson);
        } catch (Exception e) {
            Log.errorf(e, "Falha ao serializar GeminiRequest para debug: %s", e.getMessage());
        }

        return geminiClient.gerarConteudo(apiKey, request)
                .onItem().invoke(response -> {
                    if (response == null) {
                        Log.warn("Gemini returned null response object.");
                    } else {
                        var size = response.getCandidates() == null ? 0 : response.getCandidates().size();
                        Log.infof("Gemini returned candidates count: %d", size);
                    }
                })
                .onItem().transform(response -> {
                    if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                        throw new IllegalStateException("Gemini retornou resposta vazia ou sem candidates.");
                    }

                    var firstCandidate = response.getCandidates().get(0);
                    if (firstCandidate == null || firstCandidate.getContent() == null || firstCandidate.getContent().getParts() == null || firstCandidate.getContent().getParts().isEmpty()) {
                        throw new IllegalStateException("Gemini retornou candidate sem conteúdo de partes.");
                    }

                    String rawText = firstCandidate.getContent().getParts().get(0).getText();
                    Log.infof("🤖 Resposta Raw da Gemini API: %s", rawText);
                    String cleanedText = rawText.replaceAll("```json|```", "").trim();

                    try {
                        var agendamento = objectMapper.readValue(cleanedText, AgendamentoDTO.class);
                        Log.infof("✅ Agendamento parseado com sucesso: %s", agendamento);
                        return cleanedText;
                    } catch (Exception e) {
                        Log.errorf(e, "Falha ao desserializar AgendamentoDTO a partir do JSON limpo: %s", cleanedText);
                        throw new IllegalStateException("Falha ao processar a resposta do Gemini.", e);
                    }
                })
                .onFailure().invoke(err -> Log.errorf(err, "Erro ao invocar Gemini API: %s", err.getMessage()))
                .onFailure().retry().atMost(3)
                .onFailure().recoverWithItem(fallbackJson);
    }
}