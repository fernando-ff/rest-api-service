package com.telecaixa.application;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.jboss.logging.Logger;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@ApplicationScoped
@RegisterForReflection
public class CalendarIntegrationService {

    private static final Logger LOG = Logger.getLogger(CalendarIntegrationService.class);

    @Inject
    Calendar googleCalendarClient;

    @ConfigProperty(name = "google.calendar.id", defaultValue = "primary")
    String calendarId;
    @ConfigProperty(name = "google.config.path")
    String googleConfigPath;

    private String resolvedCalendarId;

    private String resolveCalendarId() {
        if (this.calendarId != null && !this.calendarId.isBlank()) {
            return this.calendarId.trim();
        }

        return ConfigProvider.getConfig()
                .getOptionalValue("google.calendar.id", String.class)
                .filter(s -> !s.isBlank())
                .or(() -> ConfigProvider.getConfig().getOptionalValue("GOOGLE_CALENDAR_ID", String.class))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse("primary");
    }

    @PostConstruct
    void init() {
        try {
            this.resolvedCalendarId = resolveCalendarId();
            LOG.infof("Resolved configuration: google.calendar.id='%s'", resolvedCalendarId);
        } catch (Exception e) {
            LOG.warn("Failed to resolve google.calendar.id", e);
            throw e;
        }
    }

    /**
     * Checks the Google Calendar grid to verify if a specific time slot is empty.
     */
    public Uni<Boolean> isTimeSlotAvailable(LocalDateTime startDateTime) {
        return Uni.createFrom().item(() -> {
            try {
                // We define a standard 1-hour appointment window
                java.time.ZonedDateTime zoneStart = startDateTime.atZone(ZoneId.systemDefault());
                java.time.ZonedDateTime zoneEnd = startDateTime.plusHours(1).atZone(ZoneId.systemDefault());

                DateTime timeMin = new DateTime(Date.from(zoneStart.toInstant()));
                DateTime timeMax = new DateTime(Date.from(zoneEnd.toInstant()));

                // Use HTTP fallback to query events (avoids Google SDK reflection issues in native image)
                String timeMinStr = zoneStart.toOffsetDateTime().toString();
                String timeMaxStr = zoneEnd.toOffsetDateTime().toString();
                boolean available = queryEventsHttp(resolvedCalendarId, timeMinStr, timeMaxStr);
                return available;
            } catch (Exception e) {
                throw new RuntimeException("Google Calendar read operation failed", e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()); // Offloads blocking I/O safely
    }

    /**
     * Commits a confirmed appointment event onto the Google Calendar grid.
     */
    public Uni<Event> bookAppointment(LocalDateTime startDateTime, String customerInfo, String serviceName) {
        return Uni.createFrom().item(() -> {
            try {
                Event event = new Event()
                        .setSummary(serviceName + " - " + customerInfo)
                        .setDescription("Agendamento efetuado automaticamente via ZapAgenda.");

                java.time.ZonedDateTime zoneStart = startDateTime.atZone(ZoneId.systemDefault());
                java.time.ZonedDateTime zoneEnd = startDateTime.plusHours(1).atZone(ZoneId.systemDefault());

                EventDateTime start = new EventDateTime().setDateTime(new DateTime(Date.from(zoneStart.toInstant())));
                EventDateTime end = new EventDateTime().setDateTime(new DateTime(Date.from(zoneEnd.toInstant())));

                event.setStart(start);
                event.setEnd(end);

                // Use HTTP fallback to insert event
                String timeStart = zoneStart.toOffsetDateTime().toString();
                String timeEnd = zoneEnd.toOffsetDateTime().toString();
                JsonObject created = insertEventHttp(resolvedCalendarId, timeStart, timeEnd, serviceName + " - " + customerInfo, "Agendamento efetuado automaticamente via ZapAgenda.");
                // Convert minimal response to Event model: set id and summary
                Event createdEvent = new Event();
                if (created.has("id")) createdEvent.setId(created.get("id").getAsString());
                if (created.has("summary")) createdEvent.setSummary(created.get("summary").getAsString());
                return createdEvent;
            } catch (Exception e) {
                throw new RuntimeException("Google Calendar write operation failed", e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private String getAccessToken() throws Exception {
        Path filePath = Paths.get(googleConfigPath).toAbsolutePath().normalize();
        try (InputStream in = new FileInputStream(filePath.toFile())) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(Collections.singleton("https://www.googleapis.com/auth/calendar"));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        }
    }

    private boolean queryEventsHttp(String calendarId, String timeMin, String timeMax) throws Exception {
        String encodedCalendar = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
        String base = String.format("https://www.googleapis.com/calendar/v3/calendars/%s/events?timeMin=%s&timeMax=%s&singleEvents=true", encodedCalendar, URLEncoder.encode(timeMin, StandardCharsets.UTF_8), URLEncoder.encode(timeMax, StandardCharsets.UTF_8));
        URL url = new URL(base);
        LOG.infof("queryEventsHttp URL=%s", base);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        String responseBody = sb.toString();
        LOG.infof("queryEventsHttp responseCode=%d body=%s", code, responseBody);
        if (code < 200 || code >= 300) {
            throw new RuntimeException("queryEventsHttp failed: HTTP " + code + " - " + responseBody);
        }
        JsonObject obj = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray items = obj.has("items") ? obj.getAsJsonArray("items") : null;
        return items == null || items.size() == 0;
    }

    private JsonObject insertEventHttp(String calendarId, String timeStart, String timeEnd, String summary, String description) throws Exception {
        String encodedCalendar = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
        String base = String.format("https://www.googleapis.com/calendar/v3/calendars/%s/events", encodedCalendar);
        URL url = new URL(base);
        LOG.infof("insertEventHttp URL=%s", base);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        JsonObject body = new JsonObject();
        body.addProperty("summary", summary);
        body.addProperty("description", description);
        JsonObject start = new JsonObject();
        start.addProperty("dateTime", timeStart);
        start.addProperty("timeZone", ZoneId.systemDefault().toString());
        JsonObject end = new JsonObject();
        end.addProperty("dateTime", timeEnd);
        end.addProperty("timeZone", ZoneId.systemDefault().toString());
        body.add("start", start);
        body.add("end", end);

        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(out.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        String responseBody = sb.toString();
        LOG.infof("insertEventHttp responseCode=%d body=%s", code, responseBody);
        if (code < 200 || code >= 300) {
            throw new RuntimeException("insertEventHttp failed: HTTP " + code + " - " + responseBody);
        }
        return JsonParser.parseString(responseBody).getAsJsonObject();
    }
}