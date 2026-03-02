package com.rafael.nailspro.webapp.application.professional;

import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentCancelledEvent;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentFinishedEvent;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentMissedEvent;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessionalAppointmentStatusUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void confirm(Long appointmentId, Long professionalId) {
        Appointment appointment = getAppointmentAndProfessionalOwnership(appointmentId, professionalId);
        appointment.confirm();
    }

    @Transactional
    public void finish(Long appointmentId, Long professionalId) {
        Appointment appointment = getAppointmentAndProfessionalOwnership(appointmentId, professionalId);

        appointment.finish();

        eventPublisher.publishEvent(
                new AppointmentFinishedEvent(
                        appointment.getId(),
                        appointment.getClient().getId(),
                        appointment.getTenantId(),
                        appointment.getTotalValue(),
                        appointment.getEndDate().atZone(appointment.getSalonZoneId())
                )
        );
    }

    @Transactional
    public void cancel(Long appointmentId, Long professionalId) {
        Appointment appointment = getAppointmentAndProfessionalOwnership(appointmentId, professionalId);

        appointment.cancel();

        eventPublisher.publishEvent(
                new AppointmentCancelledEvent(
                        appointment.getId(),
                        appointment.getTenantId(),
                        appointment.getClient().getId()
                )
        );
    }

    @Transactional
    public void miss(Long appointmentId, Long professionalId) {
        Appointment appointment = getAppointmentAndProfessionalOwnership(appointmentId, professionalId);

        appointment.miss();

        eventPublisher.publishEvent(
                new AppointmentMissedEvent(
                        appointment.getId(),
                        appointment.getTenantId(),
                        appointment.getClient().getId()
                )
        );
    }

    private Appointment getAppointmentAndProfessionalOwnership(Long appointmentId, Long professionalId) {
        return appointmentRepository
                .findAndValidateProfessionalOwnership(appointmentId, professionalId)
                .orElseThrow(() ->
                        new BusinessException("Agendamento não encontrado ou não pertence ao profissional"));
    }
}