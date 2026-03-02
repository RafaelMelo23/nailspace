package com.rafael.nailspro.webapp.application.appointment.booking;

import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.domain.repository.ClientRepository;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public void cancelAppointment(Long appointmentId, Long clientId) {
        log.info("Attempting to cancel appointment: ID={}, Client={}", appointmentId, clientId);

        Appointment appointment = appointmentRepository.findAndValidateClientOwnership(appointmentId, clientId)
                .orElseThrow(() -> new BusinessException("Esse agendamento não pertence ao usuário"));
        appointment.cancel();

        Instant twoDaysFromNow = Instant.now().plus(2, ChronoUnit.DAYS);

        if (appointment.getStartDate().isBefore(twoDaysFromNow)) {
            log.info("Late cancellation detected for Client={}. Incrementing penalty counter.", clientId);
            clientRepository.incrementCanceledAppointments(clientId);
        }

        log.info("Appointment {} successfully cancelled.", appointmentId);

        appointmentRepository.save(appointment);
    }
}