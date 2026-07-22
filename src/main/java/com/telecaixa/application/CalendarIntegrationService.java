package com.telecaixa.application;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class CalendarIntegrationService {

    private static final Logger LOG = Logger.getLogger(CalendarIntegrationService.class);

    @Inject
    Calendar googleCalendarClient;

    @ConfigProperty(name = "google.calendar.id")
    String calendarId;

    private String getCalendarId() {
        if (calendarId == null) {
            throw new IllegalStateException("google.calendar.id is not set");
        }
        String trimmed = calendarId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("google.calendar.id is empty");
        }
        return trimmed;
    }

    @PostConstruct
    void init() {
        try {
            LOG.infof("Resolved configuration: google.calendar.id='%s'", calendarId);
        } catch (Exception e) {
            LOG.warn("Failed to log google.calendar.id", e);
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

                // Query overlapping events from the Google grid
                Events events = googleCalendarClient.events().list(getCalendarId())
                        .setTimeMin(timeMin)
                        .setTimeMax(timeMax)
                        .setSingleEvents(true)
                        .execute();

                List<Event> items = events.getItems();
                return items == null || items.isEmpty();
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

                return googleCalendarClient.events().insert(getCalendarId(), event).execute();
            } catch (Exception e) {
                throw new RuntimeException("Google Calendar write operation failed", e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}