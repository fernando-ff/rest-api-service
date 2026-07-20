package com.telecaixa.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record GeminiRequest(
        @JsonProperty("contents") List<Content> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig
) {

    public GeminiRequest(String promptSistema, String promptUsuario) {
        this(
                List.of(
                        new Content("system", List.of(new Part(promptSistema))),
                        new Content("user", List.of(new Part(promptUsuario)))
                ),
                new GenerationConfig()
        );
    }

    @RegisterForReflection
    public static record Content(
            @JsonProperty("role") String role,
            @JsonProperty("parts") List<Part> parts
    ) {}

    @RegisterForReflection
    public static record Part(
            @JsonProperty("text") String text
    ) {}

    @RegisterForReflection
    public static record GenerationConfig(
            @JsonProperty("responseMimeType") String responseMimeType
    ) {
        public GenerationConfig() {
            this("application/json");
        }
    }
}
