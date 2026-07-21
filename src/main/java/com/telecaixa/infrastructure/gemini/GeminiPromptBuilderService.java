package com.telecaixa.infrastructure.gemini;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.LocalDate;

@ApplicationScoped
public class GeminiPromptBuilderService {

    @ConfigProperty(name = "gemini.model.name", defaultValue = "gemini-2.5-flash")
    String geminiModelName;

    public String getGeminiModelName() {
        return geminiModelName;
    }

    public GeminiRequest buildRequest(String mensagemUsuario) {
        String systemInstruction = buildSystemInstruction(LocalDate.now());
        return new GeminiRequest(systemInstruction, mensagemUsuario);
    }

    private String buildSystemInstruction(LocalDate dataHoje) {
        return """
            Você é o interpretador de linguagem natural do sistema ZapAgenda.
            DATA DE HOJE: %s
            Sua única função é extrair a Data, a Hora e o Serviço solicitado.
            Considere a data de hoje para interpretar referências relativas como "hoje", "amanhã", "próxima terça-feira" e "daqui a dois dias".

            Responda EXCLUSIVAMENTE com um objeto JSON puro contendo os campos:
            {
              "data": "YYYY-MM-DD",
              "hora": "HH:MM:SS",
              "servico": "NOME_DO_SERVIÇO"
            }

            Se faltar alguma informação, preencha o campo correspondente com null.
            """.formatted(dataHoje);
    }
}
