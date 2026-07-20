package com.telecaixa.infrastructure.gemini;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public class GeminiSpecs {

    // --- CONTRATO DE ENTRADA (O que enviamos para o Google) ---
    public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
        public GeminiRequest(String promptSistema, String promptUsuario) {
            this(
                List.of(
                    new Content("system", List.of(new Part(promptSistema))),
                    new Content("user", List.of(new Part(promptUsuario)))
                ),
                new GenerationConfig()
            );
        }

        public static record Content(String role, List<Part> parts) {}
    }

    public static record Part(String text) {}

    public static record GenerationConfig(String responseMimeType) {
        public GenerationConfig() {
            this("application/json");
        }
    }

    // --- CONTRATO DE SAÍDA (O que o Google nos devolve) ---
    public record GeminiResponse(List<Candidate> candidates) {}
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
}
