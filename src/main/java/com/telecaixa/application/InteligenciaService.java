package com.telecaixa.application;

import com.telecaixa.infrastructure.gemini.GeminiRestClient;
import com.telecaixa.infrastructure.gemini.GeminiSpecs.GeminiRequest;

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

        return geminiClient.gerarConteudo(apiKey, request)
                .onItem().transform(response -> {
                    // Extrai o texto limpo retornado pela API do Gemini
                    String text = response.candidates().get(0).content().parts().get(0).text();
                    return text.replaceAll("```json|```", "").trim();
                })
                .onFailure().retry().atMost(3) // Resiliência: tenta até 3x em caso de 503 ou erro de rede
                .onFailure().recoverWithItem(fallbackJson); // Retorna o JSON de fallback se a API falhar
    }
}