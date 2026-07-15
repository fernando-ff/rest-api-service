package com.telecaixa.interfaces.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class TelegramWebhookResourceTest {

    @Inject
    TelegramWebhookResource resource;

    @Test
    void shouldParseSchedulingPayloadFromGeminiJson() throws Exception {
        String payload = "{\"data\":\"2026-07-16\",\"hora\":\"15:00:00\",\"servico\":\"corte\"}";

        TelegramWebhookResource.DadosAgendamento agendamento = resource.parseDadosAgendamento(payload);

        assertEquals("2026-07-16", agendamento.data());
        assertEquals("15:00:00", agendamento.hora());
        assertEquals("corte", agendamento.servico());
    }
}
