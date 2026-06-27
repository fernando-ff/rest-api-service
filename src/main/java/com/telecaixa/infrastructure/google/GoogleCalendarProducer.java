package com.telecaixa.infrastructure.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent; // Importe o escopo correto
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.InputStream;
import java.util.Collections;

@ApplicationScoped // A classe fábrica continua ApplicationScoped
public class GoogleCalendarProducer {

    @ConfigProperty(name = "google.config.path")
    String configPath;

    @Produces
    @Dependent // <-- Mude de @ApplicationScoped para @Dependent aqui!
    public Calendar produceGoogleCalendar() {
        try {
            InputStream in = GoogleCalendarProducer.class.getResourceAsStream(configPath);
            if (in == null) {
                throw new IllegalStateException("Erro Crítico: Arquivo de configuração " + configPath + " não encontrado.");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("ZapAgenda")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao inicializar o cliente do Google Calendar", e);
        }
    }
}