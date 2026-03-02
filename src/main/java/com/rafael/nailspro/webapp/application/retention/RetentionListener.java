package com.rafael.nailspro.webapp.application.retention;

import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentFinishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RetentionListener {

    private final VisitPredictionService forecastUseCase;
    private final AppointmentRepository appointmentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFinished(AppointmentFinishedEvent event) {
        Appointment appointment = appointmentRepository.findById(event.appointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        boolean hasMaintenanceInterval =
                appointment.getMainSalonService().getMaintenanceIntervalDays() != null
                        || appointment.getAddOns().stream()
                        .anyMatch(addon ->
                                addon.getService().getMaintenanceIntervalDays() != null
                        );

        if (hasMaintenanceInterval) {
            forecastUseCase.createForecast(appointment);
        }
    }
}
