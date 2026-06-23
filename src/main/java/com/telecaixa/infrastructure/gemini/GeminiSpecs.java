package com.telecaixa.infrastructure.gemini;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public class GeminiSpecs {

    // --- CONTRATO DE ENTRADA (O que enviamos para o Google) ---
    public record GeminiRequest(List<Content> contents, SystemInstruction systemInstruction) {
        public GeminiRequest(String promptSistema, String promptUsuario) {
            this(
                List.of(new Content(List.of(new Part(promptUsuario)))),
                new SystemInstruction(List.of(new Part(promptSistema)))
            );
        }
    }
    public record SystemInstruction(List<Part> parts) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}

    // --- CONTRATO DE SAÍDA (O que o Google nos devolve) ---
    public record GeminiResponse(List<Candidate> candidates) {
        public String getTextoExtraido() {
            if (candidates != null && !candidates.isEmpty()) {
                var parts = candidates.get(0).content().parts();
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).text();
                }
            }
            throw new IllegalStateException("A IA retornou um payload vazio.");
        }
    }
    public record Candidate(ContentOutput content) {}
    public record ContentOutput(List<Part> parts) {}
}