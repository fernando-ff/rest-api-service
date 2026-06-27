package com.telecaixa.infrastructure.google;

import io.quarkus.runtime.annotations.RegisterForReflection;

// Registra as classes de modelo do Google para que a GraalVM não as apague no build nativo
@RegisterForReflection(targets = {
    com.google.api.services.calendar.model.Event.class,
    com.google.api.services.calendar.model.EventDateTime.class,
    com.google.api.services.calendar.model.Events.class,
    com.google.api.client.util.DateTime.class
})
public class GoogleReflectionConfig {
    // Classe vazia usada apenas para segurar a anotação de metadados do Quarkus
}