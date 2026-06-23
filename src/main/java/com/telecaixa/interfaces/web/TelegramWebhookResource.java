package com.telecaixa.interfaces.web;

import com.telecaixa.application.InteligenciaService;
import com.telecaixa.infrastructure.telegram.TelegramSpecs.*;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/v1/telegram")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TelegramWebhookResource {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramWebhookResource.class);

    @Inject
    InteligenciaService inteligenciaService;

    @POST
    @Path("/webhook")
    public Uni<Response> receberUpdate(TelegramUpdate update) {
        LOG.info("Recebendo update do Telegram: {}", update);

        if (update == null || update.message() == null || update.message().text() == null) {
            LOG.warn("Update inválido recebido no webhook: payload sem message/text");
            return Uni.createFrom().item(Response.ok().build());
        }

        Long chatId = update.message().chat().id();
        String textoUsuario = update.message().text();

        LOG.info("Processando mensagem do chat {}: {}", chatId, textoUsuario);

        if ("/start".equalsIgnoreCase(textoUsuario.trim())) {
            LOG.info("Comando /start recebido no chat {}", chatId);
            var boasVindas = new TelegramWebhookResponse(chatId, "👋 Olá! Eu sou o ZapAgenda. Diga o que deseja agendar, o dia e o horário!");
            return Uni.createFrom().item(Response.ok(boasVindas).build());
        }

        return inteligenciaService.extrairDadosAgendamento(textoUsuario)
                .onItem().transform(jsonDaIA -> {
                    String mensagemRetorno = "🤖 O Gemini interpretou seu pedido assim:\n\n" + jsonDaIA;
                    LOG.info("Resposta preparada para o chat {}: {}", chatId, mensagemRetorno);
                    return Response.ok(new TelegramWebhookResponse(chatId, mensagemRetorno)).build();
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.error("Erro ao processar mensagem do chat {}: {}", chatId, err.getMessage(), err);
                    String msgErro = "⚠️ Erro ao acionar a inteligência artificial: " + err.getMessage();
                    return Response.ok(new TelegramWebhookResponse(chatId, msgErro)).build();
                });
    }
}