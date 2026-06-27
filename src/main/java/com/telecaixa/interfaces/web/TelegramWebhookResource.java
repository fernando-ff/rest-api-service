package com.telecaixa.interfaces.web;

import com.telecaixa.application.CalendarIntegrationService;
import com.telecaixa.application.InteligenciaService;
import com.telecaixa.infrastructure.telegram.TelegramSpecs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@Path("/v1/telegram")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TelegramWebhookResource {

    private static final Logger LOG = Logger.getLogger(TelegramWebhookResource.class);

    @Inject
    InteligenciaService inteligenciaService;

    @Inject
    CalendarIntegrationService calendarService;

    @Inject
    ObjectMapper objectMapper; // Injetado nativamente pelo Quarkus (Jackson)

    // Record local para mapear o JSON estrito que o Gemini devolve
    public record DadosAgendamento(String data, String hora, String servico) {}

    @POST
    @Path("/webhook")
    public Uni<Response> receberUpdate(TelegramUpdate update) {
        LOG.infof("📥 Recebido webhook do Telegram: %s", update);
        
        // Validação básica do payload do Telegram
        if (update == null || update.message() == null || update.message().text() == null) {
            LOG.warnf("⚠️ Payload inválido do Telegram - update nulo ou sem mensagem");
            return Uni.createFrom().item(Response.ok().build());
        }

        Long chatId = update.message().chat().id();
        String textoUsuario = update.message().text();
        LOG.infof("💬 ChatID: %d | Texto recebido: '%s'", chatId, textoUsuario);

        // Trata o comando inicial do Bot
        if ("/start".equalsIgnoreCase(textoUsuario.trim())) {
            LOG.infof("🚀 Comando /start recebido do ChatID: %d", chatId);
            var boasVindas = new TelegramWebhookResponse(chatId, "👋 Olá! Eu sou o ZapAgenda. Diga o serviço, o dia e o horário que deseja marcar!");
            return Uni.createFrom().item(Response.ok(boasVindas).build());
        }

        // --- JUNÇÃO DO PIPELINE REATIVO DE PONTA A PONTA ---
        LOG.infof("🔄 Iniciando pipeline de agendamento para ChatID: %d", chatId);
        return inteligenciaService.extrairDadosAgendamento(textoUsuario)
                .onItem().transformToUni(jsonIa -> {
                    LOG.debugf("🧠 Resposta da IA: %s", jsonIa);
                    try {
                        // 1. Converte a String JSON da IA em um Objeto Java com segurança
                        DadosAgendamento agendamento = objectMapper.readValue(jsonIa, DadosAgendamento.class);
                        LOG.infof("📝 Dados parseados - Serviço: %s, Data: %s, Hora: %s", agendamento.servico(), agendamento.data(), agendamento.hora());
                        
                        if (agendamento.data() == null || agendamento.hora() == null) {
                            LOG.warnf("⚠️ Data ou hora nula para ChatID: %d", chatId);
                            return Uni.createFrom().item(new TelegramWebhookResponse(chatId, "⚠️ Não consegui entender a data ou o horário. Pode especificar melhor?"));
                        }

                        // Une data e hora em um LocalDateTime do Java
                        LocalDateTime dataHoraDesejada = LocalDateTime.parse(agendamento.data() + "T" + agendamento.hora());
                        LOG.infof("⏰ Data/Hora desejada: %s", dataHoraDesejada);

                        // 2. Chuta a validação para o serviço do Google Calendar (Verifica Disponibilidade)
                        LOG.infof("🔍 Verificando disponibilidade para: %s", dataHoraDesejada);
                        return calendarService.isTimeSlotAvailable(dataHoraDesejada)
                                .onItem().transformToUni(disponivel -> {
                                    LOG.infof("✔️ Slot disponível: %b", disponivel);
                                    if (!disponivel) {
                                        LOG.warnf("❌ Horário indisponível para ChatID: %d - %s", chatId, dataHoraDesejada);
                                        return Uni.createFrom().item(new TelegramWebhookResponse(chatId, "❌ Desculpe, esse horário já está preenchido na agenda. Escolha outro!"));
                                    }

                                    // 3. Se estiver livre, grava o agendamento no Google Calendar
                                    String infoCliente = "Cliente ChatID: " + chatId;
                                    LOG.infof("📅 Realizando booking para serviço: %s, Data: %s, InfoCliente: %s", agendamento.servico(), dataHoraDesejada, infoCliente);
                                    return calendarService.bookAppointment(dataHoraDesejada, infoCliente, agendamento.servico())
                                            .onItem().transform(eventoCriado -> {
                                                LOG.infof("✅ Agendamento criado com sucesso para ChatID: %d - Evento: %s", chatId, eventoCriado);
                                                // Retorno de sucesso absoluto
                                                return new TelegramWebhookResponse(chatId, "✅ Confirmado! Seu agendamento de '" + agendamento.servico() + "' foi salvo para o dia " + agendamento.data() + " às " + agendamento.hora() + ".");
                                            });
                                });

                    } catch (Exception e) {
                        // Captura erros de parsing do JSON da IA
                        LOG.errorf(e, "❌ Erro ao processar JSON da IA para ChatID: %d - Payload: %s", chatId, jsonIa);
                        return Uni.createFrom().item(new TelegramWebhookResponse(chatId, "⚠️ Houve uma falha ao processar os dados do agendamento."));
                    }
                })
                // Converte o objeto final de resposta do Telegram em um HTTP Status 200 OK
                .onItem().transform(respostaBot -> {
                    LOG.debugf("📤 Enviando resposta ao Telegram para ChatID: %d", chatId);
                    return Response.ok(respostaBot).build();
                })
                // Tratamento de falhas genéricas do ecossistema (Quedas de conexão, APIs fora do ar, etc)
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "🚨 Erro na pipeline para ChatID: %d", chatId);
                    String erroMsg = "⚠️ Erro interno no servidor: " + err.getMessage();
                    return Response.ok(new TelegramWebhookResponse(chatId, erroMsg)).build();
                });
    }
}