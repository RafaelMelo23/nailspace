package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.domain.repository.SalonProfileRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.event.AppointmentCancelledEvent;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.event.AppointmentFinishedEvent;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.event.AppointmentMissedEvent;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RecordApplicationEvents
class ProfessionalAppointmentStatusUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private ProfessionalAppointmentStatusUseCase useCase;

    @Autowired
    private ApplicationEvents events;

    private Professional professional;
    private Client client;
    private SalonProfile salonProfile;
    private SalonService service;

    @BeforeEach
    void setUp() {
        salon(TestProfessionalFactory.builder().build(), "tenant-test");
    }

    private void salon(Professional pro, String tenant) {
        TenantContext.setTenant(tenant);
        this.professional = professionalRepository.save(pro);
        this.salonProfile = salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional, tenant));
        this.client = clientRepository.save(TestClientFactory.builder().build());
        this.service = salonServiceRepository.save(TestSalonServiceFactory.builder().build());
    }

    @Test
    void shouldConfirmAppointment() {
        var appointment = appointmentRepository.save(TestAppointmentFactory.standardForIt(client, professional, service));

        useCase.confirm(appointment.getId(), professional.getId());

        var updated = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(updated.getAppointmentStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void shouldFinishAppointmentAndPublishEvent() {
        var appointment = appointmentRepository.save(TestAppointmentFactory.pastForIt(client, professional, service, AppointmentStatus.CONFIRMED));

        useCase.finish(appointment.getId(), professional.getId());

        var updated = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(updated.getAppointmentStatus()).isEqualTo(AppointmentStatus.FINISHED);
        assertThat(events.stream(AppointmentFinishedEvent.class)).hasSize(1);
    }

    @Test
    void shouldCancelAppointmentAndPublishEvent() {
        var appointment = appointmentRepository.save(TestAppointmentFactory.futureForIt(client, professional, service, AppointmentStatus.CONFIRMED));

        useCase.cancel(appointment.getId(), professional.getId());

        var updated = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(updated.getAppointmentStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(events.stream(AppointmentCancelledEvent.class)).hasSize(1);
    }

    @Test
    void shouldMissAppointmentAndPublishEvent() {
        var appointment = appointmentRepository.save(TestAppointmentFactory.pastForIt(client, professional, service, AppointmentStatus.CONFIRMED));

        useCase.miss(appointment.getId(), professional.getId());

        var updated = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(updated.getAppointmentStatus()).isEqualTo(AppointmentStatus.MISSED);
        assertThat(events.stream(AppointmentMissedEvent.class)).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenAppointmentNotBelongsToProfessional() {
        Professional pro2 = professionalRepository.save(TestProfessionalFactory.builder().build());
        var appointment = appointmentRepository.save(TestAppointmentFactory.standardForIt(client, professional, service));

        assertThatThrownBy(() -> useCase.confirm(appointment.getId(), pro2.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Agendamento não encontrado ou não pertence ao profissional");
    }

    @Test
    void shouldRespectTenantIsolation() {
        var appointment = appointmentRepository.save(TestAppointmentFactory.standardForIt(client, professional, service));

        TenantContext.setTenant("outro-tenant");

        assertThatThrownBy(() -> useCase.confirm(appointment.getId(), professional.getId()))
                .isInstanceOf(BusinessException.class);
    }
}