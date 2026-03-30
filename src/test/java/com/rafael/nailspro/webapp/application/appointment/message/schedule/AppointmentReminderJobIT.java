package com.rafael.nailspro.webapp.application.appointment.message.schedule;

import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionMessageStatus;
import com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageType;
import com.rafael.nailspro.webapp.domain.whatsapp.SentMessageResult;
import com.rafael.nailspro.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestAppointmentFactory;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
import com.rafael.nailspro.webapp.support.factory.TestProfessionalFactory;
import com.rafael.nailspro.webapp.support.factory.TestSalonProfileFactory;
import com.rafael.nailspro.webapp.support.factory.TestSalonServiceFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
class AppointmentReminderJobIT extends BaseIntegrationTest {

    @Autowired
    private AppointmentReminderJob job;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean(name = "whatsappProvider")
    private WhatsappProvider whatsappProvider;

    @Test
    void shouldProcessRemindersForUpcomingAppointments() {
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        var client = clientRepository.save(TestClientFactory.standardForIt());
        var service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt());
        
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional, "tenant-test"));

        // Appointment within 5 hours
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        var appointment = TestAppointmentFactory.atSpecificTimeForIt(
                start, start.plus(1, ChronoUnit.HOURS), client, professional, service, com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus.PENDING);
        appointmentRepository.save(appointment);

        // Success result for mock
        when(whatsappProvider.sendText(anyString(), anyString(), anyString()))
                .thenReturn(new SentMessageResult("msg-123", EvolutionMessageStatus.SERVER_ACK));

        job.sendReminders();

        verify(whatsappProvider, times(1)).sendText(anyString(), anyString(), anyString());

        var message = whatsappMessageRepository.findByAppointmentIdAndMessageType(appointment.getId(), WhatsappMessageType.REMINDER);
        assertThat(message).isPresent();
        assertThat(message.get().getMessageStatus().name()).isEqualTo("SENT");
    }

    @Test
    void shouldNotRemindIfAlreadySent() {
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        var client = clientRepository.save(TestClientFactory.standardForIt());
        var service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt());

        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional, "tenant-test"));

        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        var appointment = appointmentRepository.save(TestAppointmentFactory.atSpecificTimeForIt(
                start, start.plus(1, ChronoUnit.HOURS), client, professional, service, com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus.PENDING));

        // Pretend a reminder was already sent
        var message = whatsappMessageRepository.save(com.rafael.nailspro.webapp.domain.model.WhatsappMessage.builder()
                .appointment(appointment)
                .messageType(WhatsappMessageType.REMINDER)
                .messageStatus(com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageStatus.SENT)
                .destinationNumber(client.getPhoneNumber())
                .attempts(1)
                .build());

        job.sendReminders();

        verify(whatsappProvider, never()).sendText(anyString(), anyString(), anyString());
    }

    @Test
    void shouldHandleDifferentTenants() {
        // Tenant A
        TenantContext.setTenant("tenant-a");
        var profA = professionalRepository.save(TestProfessionalFactory.standardForIt("tenant-a"));
        var clientA = clientRepository.save(TestClientFactory.standardForIt("tenant-a"));
        var serviceA = salonServiceRepository.save(TestSalonServiceFactory.standardForIt("tenant-a"));
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(profA, "tenant-a"));
        
        Instant startA = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        var apA = TestAppointmentFactory.atSpecificTimeForIt(
                startA, startA.plus(1, ChronoUnit.HOURS), clientA, profA, serviceA, com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus.PENDING);
        apA.setTenantId("tenant-a");
        appointmentRepository.save(apA);

        // Tenant B
        TenantContext.setTenant("tenant-b");
        var profB = professionalRepository.save(TestProfessionalFactory.standardForIt("tenant-b"));
        var clientB = clientRepository.save(TestClientFactory.standardForIt("tenant-b"));
        var serviceB = salonServiceRepository.save(TestSalonServiceFactory.standardForIt("tenant-b"));
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(profB, "tenant-b"));
        
        Instant startB = Instant.now().plus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        var apB = TestAppointmentFactory.atSpecificTimeForIt(
                startB, startB.plus(1, ChronoUnit.HOURS), clientB, profB, serviceB, com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus.PENDING);
        apB.setTenantId("tenant-b");
        appointmentRepository.save(apB);

        Instant now = Instant.now();
        Instant windowEnd = now.plus(5, ChronoUnit.HOURS);
        var results = appointmentRepository.findAppointmentsNeedingReminder(now, windowEnd);
        log.info("DEBUG: Repo query returned {} appointments", results.size());
        
        job.sendReminders();

        verify(whatsappProvider, times(2)).sendText(anyString(), anyString(), anyString());
    }
}