package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.BusyIntervalService;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.event.AppointmentCancelledEvent;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.event.AppointmentFinishedEvent;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.event.AppointmentMissedEvent;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessionalAppointmentStatusUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BusyIntervalService busyIntervalService;
    private final SalonProfileService salonProfileService;

    @Transactional
    public void confirm(Long appointmentId, Long professionalId) {
        Appointment appointment = getAppointmentAndProfessionalOwnership(appointmentId, professionalId);
        appointment.confirm();
        evictCache(appointment);
    }

    @Transactional
    public void finish(Long appointmentId, Long professionalId) {
        Appointment appointment = getAppointmentAndProfessionalOwnership(appointmentId, professionalId);

        appointment.finish();
        evictCache(appointment);

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
        evictCache(appointment);

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
        evictCache(appointment);

        eventPublisher.publishEvent(
                new AppointmentMissedEvent(
                        appointment.getId(),
                        appointment.getTenantId(),
                        appointment.getClient().getId()
                )
        );
    }

    private void evictCache(Appointment appointment) {
        busyIntervalService.evictCacheForAppointment(appointment, salonProfileService.getSalonZoneId(appointment.getTenantId()));
    }

    private Appointment getAppointmentAndProfessionalOwnership(Long appointmentId, Long professionalId) {
        return appointmentRepository
                .findAndValidateProfessionalOwnership(appointmentId, professionalId)
                .orElseThrow(() ->
                        new BusinessException("Agendamento não encontrado ou não pertence ao profissional"));
    }
}