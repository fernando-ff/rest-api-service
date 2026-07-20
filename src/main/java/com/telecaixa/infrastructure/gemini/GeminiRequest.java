package com.telecaixa.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public class GeminiRequest {

    @JsonProperty("system_instruction")
    private SystemInstruction systemInstruction;

    @JsonProperty("contents")
    private List<Content> contents;

    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    public GeminiRequest() {
    }

    public GeminiRequest(String promptSistema, String promptUsuario) {
        this.systemInstruction = new SystemInstruction(List.of(new Part(promptSistema)));
        this.contents = List.of(new Content("user", List.of(new Part(promptUsuario))));
        this.generationConfig = new GenerationConfig();
    }

    public SystemInstruction getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(SystemInstruction systemInstruction) {
        this.systemInstruction = systemInstruction;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    @RegisterForReflection
    public static class SystemInstruction {

        @JsonProperty("parts")
        private List<Part> parts;

        public SystemInstruction() {
        }

        public SystemInstruction(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    @RegisterForReflection
    public static class Content {

        @JsonProperty("role")
        private String role;

        @JsonProperty("parts")
        private List<Part> parts;

        public Content() {
        }

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    @RegisterForReflection
    public static class Part {

        @JsonProperty("text")
        private String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    @RegisterForReflection
    public static class GenerationConfig {

        @JsonProperty("responseMimeType")
        private String responseMimeType;

        public GenerationConfig() {
            this.responseMimeType = "application/json";
        }

        public GenerationConfig(String responseMimeType) {
            this.responseMimeType = responseMimeType;
        }

        public String getResponseMimeType() {
            return responseMimeType;
        }

        public void setResponseMimeType(String responseMimeType) {
            this.responseMimeType = responseMimeType;
        }
    }
}
