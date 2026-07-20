package com.telecaixa.infrastructure.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.Produces;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@ApplicationScoped
public class GoogleCalendarProducer {

    private static final Logger LOG = Logger.getLogger(GoogleCalendarProducer.class);

    @ConfigProperty(name = "google.config.path")
    String configPathStr;

    @Produces
    @Dependent
    public Calendar produceGoogleCalendar() {
        // Converte o caminho relativo em absoluto baseado no diretório de execução atual (~/app)
        Path filePath = Paths.get(configPathStr).toAbsolutePath().normalize();
        File configFile = filePath.toFile();

        LOG.infof("Carregando credenciais do Google Calendar a partir de: %s", filePath);

        if (!configFile.exists() || !configFile.isFile()) {
            throw new IllegalStateException(
                String.format("Erro Crítico: Arquivo de configuração não encontrado no caminho: %s", filePath)
            );
        }

        if (!configFile.canRead()) {
            throw new IllegalStateException(
                String.format("Erro Crítico: Sem permissão de leitura para o arquivo: %s", filePath)
            );
        }

        try (InputStream in = new FileInputStream(configFile)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("ZapAgenda")
                    .build();

        } catch (Exception e) {
            LOG.errorf(e, "Erro ao inicializar Google Credentials em %s", filePath);
            throw new RuntimeException("Falha ao inicializar o cliente do Google Calendar", e);
        }
    }
}