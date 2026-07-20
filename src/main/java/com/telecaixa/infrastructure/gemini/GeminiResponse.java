package com.telecaixa.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        @JsonProperty("candidates") List<Candidate> candidates
) {

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Candidate(
            @JsonProperty("content") Content content
    ) {}

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Content(
            @JsonProperty("parts") List<Part> parts
    ) {}

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Part(
            @JsonProperty("text") String text
    ) {}
}
