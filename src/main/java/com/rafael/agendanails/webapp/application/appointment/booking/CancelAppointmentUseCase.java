package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.BusyIntervalService;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final BusyIntervalService busyIntervalService;
    private final SalonProfileService salonProfileService;

    @Transactional
    public void cancelAppointment(Long appointmentId, Long clientId) {
        log.info("Attempting to cancel appointment: ID={}, Client={}", appointmentId, clientId);

        Appointment appointment = appointmentRepository.findAndValidateClientOwnership(appointmentId, clientId)
                .orElseThrow(() -> new BusinessException("Esse agendamento não foi encontrado."));

        appointment.cancel();

        log.info("Appointment {} successfully cancelled.", appointmentId);

        appointmentRepository.save(appointment);
        busyIntervalService.evictCacheForAppointment(appointment, salonProfileService.getSalonZoneId(appointment.getTenantId()));
    }
}