package com.rafael.nailspro.webapp.application.appointment.message;

import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final AppointmentMessagingUseCase messagingUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleConfirmedAppointment(AppointmentConfirmedEvent appointmentConfirmedEvent) {

        messagingUseCase.sendAppointmentConfirmationMessageAsync(
                appointmentConfirmedEvent.appointmentId()
        );
    }
}
