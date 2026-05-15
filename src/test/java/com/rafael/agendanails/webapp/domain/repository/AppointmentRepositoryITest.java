package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.SalonService;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestAppointmentFactory;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
import com.rafael.agendanails.webapp.support.factory.TestProfessionalFactory;
import com.rafael.agendanails.webapp.support.factory.TestSalonServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentRepositoryITest extends BaseIntegrationTest {

    private Professional professional;
    private Client client;
    private SalonService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenant("tenant-test");
        professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        client = clientRepository.save(TestClientFactory.standardForIt());
        service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt());
    }

    @Test
    void shouldNotFindAppointmentFromAnotherTenant() {
        TenantContext.setTenant("tenant-a");
        Professional proA = professionalRepository.save(TestProfessionalFactory.standardForIt("tenant-a"));
        Client clientA = clientRepository.save(TestClientFactory.standardForIt("tenant-a"));
        SalonService serviceA = salonServiceRepository.save(TestSalonServiceFactory.standardForIt("tenant-a"));
        
        Appointment appointmentA = TestAppointmentFactory.standardForIt(clientA, proA, serviceA);
        appointmentA.assignTenant("tenant-a");
        appointmentA = appointmentRepository.save(appointmentA);
        Long appointmentId = appointmentA.getId();

        TenantContext.setTenant("tenant-b");
        Page<Appointment> found = appointmentRepository.findByClientId(clientA.getId(), PageRequest.of(0, 10));

        assertThat(found.getContent()).isEmpty();
    }

    @Test
    void shouldFindOverlappingAppointmentsCorrectly() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant start = now.plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        Appointment existing = TestAppointmentFactory.atSpecificTimeForIt(start, end, client, professional, service, AppointmentStatus.CONFIRMED);
        appointmentRepository.save(existing);

        List<Appointment> overlaps1 = appointmentRepository.findBusyAppointmentsInRange(
                professional.getId(), start, end, List.of(AppointmentStatus.CONFIRMED));

        List<Appointment> overlaps2 = appointmentRepository.findBusyAppointmentsInRange(
                professional.getId(), start.minus(30, ChronoUnit.MINUTES), start.plus(30, ChronoUnit.MINUTES), List.of(AppointmentStatus.CONFIRMED));

        List<Appointment> overlaps3 = appointmentRepository.findBusyAppointmentsInRange(
                professional.getId(), end.minus(30, ChronoUnit.MINUTES), end.plus(30, ChronoUnit.MINUTES), List.of(AppointmentStatus.CONFIRMED));

        List<Appointment> overlaps4 = appointmentRepository.findBusyAppointmentsInRange(
                professional.getId(), end, end.plus(1, ChronoUnit.HOURS), List.of(AppointmentStatus.CONFIRMED));

        assertThat(overlaps1).hasSize(1);
        assertThat(overlaps2).hasSize(1);
        assertThat(overlaps3).hasSize(1);
        assertThat(overlaps4).isEmpty();
    }

    @Test
    void shouldFindAndValidateClientOwnership() {
        Appointment appointment = appointmentRepository.save(TestAppointmentFactory.standardForIt(client, professional, service));

        Optional<Appointment> found = appointmentRepository.findAndValidateClientOwnership(appointment.getId(), client.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(appointment.getId());
    }

    @Test
    void findAppointmentsNeedingReminderShouldIgnoreTenantFilter() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant windowEnd = now.plus(24, ChronoUnit.HOURS);

        TenantContext.setTenant("tenant-a");
        Professional proA = professionalRepository.save(TestProfessionalFactory.standardForIt("tenant-a"));
        Client clientA = clientRepository.save(TestClientFactory.standardForIt("tenant-a"));
        SalonService serviceA = salonServiceRepository.save(TestSalonServiceFactory.standardForIt("tenant-a"));
        
        Appointment appA = TestAppointmentFactory.atSpecificTimeForIt(
                now.plus(1, ChronoUnit.HOURS),
                now.plus(2, ChronoUnit.HOURS),
                clientA, proA, serviceA, AppointmentStatus.CONFIRMED);
        appA.assignTenant("tenant-a");
        appointmentRepository.save(appA);

        TenantContext.setTenant("tenant-b");
        Professional proB = professionalRepository.save(TestProfessionalFactory.standardForIt("tenant-b"));
        Client clientB = clientRepository.save(TestClientFactory.standardForIt("tenant-b"));
        SalonService serviceB = salonServiceRepository.save(TestSalonServiceFactory.standardForIt("tenant-b"));
        
        Appointment appB = TestAppointmentFactory.atSpecificTimeForIt(
                now.plus(1, ChronoUnit.HOURS),
                now.plus(2, ChronoUnit.HOURS),
                clientB, proB, serviceB, AppointmentStatus.CONFIRMED);
        appB.assignTenant("tenant-b");
        appointmentRepository.save(appB);

        TenantContext.setTenant("tenant-test");
        List<Appointment> results = appointmentRepository.findAppointmentsNeedingReminder(now, windowEnd);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }
}