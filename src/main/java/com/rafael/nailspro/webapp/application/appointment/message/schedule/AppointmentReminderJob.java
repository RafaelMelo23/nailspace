package com.rafael.nailspro.webapp.application.appointment.message.schedule;

import com.rafael.nailspro.webapp.application.appointment.message.AppointmentMessagingUseCase;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class AppointmentReminderJob {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMessagingUseCase messagingUseCase;

    @IgnoreTenantFilter
    @Scheduled(cron = "0 0/15 * * * *")
    public void scheduleReminders() {
        Instant startTime = Instant.now();
        log.info("Starting appointment reminder job");
        try {
            int processed = sendReminders();

            long durationMs = ChronoUnit.MILLIS.between(startTime, Instant.now());

            log.info("Appointment reminder job completed: {} reminders processed in {} ms",
                    processed, durationMs);

        } catch (Exception ex) {
            log.error("Appointment reminder job failed", ex);
        }
    }

    private int sendReminders() {

        Instant windowStart = Instant.now();
        Instant windowEnd = windowStart.plus(5, ChronoUnit.HOURS);

        List<Appointment> upcomingAppointments =
                appointmentRepository.findAppointmentsNeedingReminder(windowStart, windowEnd);

        if (upcomingAppointments.isEmpty()) {
            log.debug("No appointments requiring reminders between {} and {}", windowStart, windowEnd);
            return 0;
        }

        log.info("Found {} appointments requiring reminders", upcomingAppointments.size());

        int processed = 0;

        for (Appointment ap : upcomingAppointments) {
            try {
                messagingUseCase.sendAppointmentReminderMessage(ap.getId());
                processed++;
            } catch (Exception e) {
                log.error("Failed to send reminder for appointmentId={}", ap.getId(), e);
            }
        }

        return processed;
    }
}