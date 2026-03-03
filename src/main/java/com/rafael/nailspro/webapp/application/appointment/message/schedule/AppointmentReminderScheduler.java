package com.rafael.nailspro.webapp.application.appointment.message.schedule;

import com.rafael.nailspro.webapp.application.appointment.message.AppointmentMessagingUseCase;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMessagingUseCase messagingUseCase;
    private final EntityManagerFactory entityManagerFactory;

    @Scheduled(cron = "0 0/15 * * * *")
    public void scheduleReminders() {
        log.info("Initiating search for appointment reminders");
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            Session session = em.unwrap(Session.class);
            session.disableFilter("tenantFilter");

            sendReminders();
        }
    }

    private void sendReminders() {
        Instant windowStart = Instant.now();
        Instant nowPlusFiveHours = windowStart.plus(5, ChronoUnit.HOURS);
        List<Appointment> upcomingAppointments =
                appointmentRepository.findAppointmentsNeedingReminder(windowStart, nowPlusFiveHours);

        upcomingAppointments.forEach(ap -> {
            try {
                messagingUseCase.sendAppointmentReminderMessage(ap.getId());
            } catch (Exception e) {
                log.error("Failed to process reminder for appointment ID: {}", ap.getId(), e);
            }
        });
    }
}