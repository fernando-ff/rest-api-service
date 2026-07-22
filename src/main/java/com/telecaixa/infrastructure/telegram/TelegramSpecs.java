package com.telecaixa.infrastructure.telegram;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TelegramSpecs {

    // --- CONTRATO DE ENTRADA (Mapeia o JSON que vem do Telegram) ---
    public record TelegramUpdate(Long update_id, Message message) {}
    public record Message(String text, Chat chat, User from) {}
    public record Chat(Long id) {}
    public record User(Long id, String first_name, String last_name, String username) {}

    // --- CONTRATO DE SAÍDA (O formato que o Telegram espera no corpo da resposta) ---
    public record TelegramWebhookResponse(String method, Long chat_id, String text) {
        public TelegramWebhookResponse(Long chatId, String text) {
            this("sendMessage", chatId, text);
        }
    }
}