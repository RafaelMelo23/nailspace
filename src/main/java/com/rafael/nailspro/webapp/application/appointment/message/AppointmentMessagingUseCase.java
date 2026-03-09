package com.rafael.nailspro.webapp.application.appointment.message;

import com.rafael.nailspro.webapp.application.messages.AppointmentMessageBuilder;
import com.rafael.nailspro.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.nailspro.webapp.domain.whatsapp.SentMessageResult;
import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationStatus;
import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationType;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationStatus.FAILED;
import static com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationType.CONFIRMATION;
import static com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationType.REMINDER;

@Log4j2
@Service
@RequiredArgsConstructor
public class AppointmentMessagingUseCase {

    private final WhatsappProvider whatsappProvider;
    private final AppointmentMessageBuilder messageBuilder;
    private final AppointmentNotificationService appointmentNotificationService;
    private final AppointmentRepository appointmentRepository;

    public void processNotification(Long appointmentId, AppointmentNotificationType type) {
        var notification = appointmentNotificationService.prepareNotification(appointmentId, type);

        try {
            var appointment = appointmentRepository.findFullById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            var message = buildMessage(appointment, type);

            SentMessageResult sentMessageResult = dispatchMessage(
                    notification.getAppointment().getTenantId(),
                    message,
                    notification.getDestinationNumber()
            );

            appointmentNotificationService.updateNotificationStatus(
                    AppointmentNotificationStatus.fromEvolutionStatus(sentMessageResult.status()),
                    null,
                    sentMessageResult.messageId(),
                    notification.getId()
            );
        } catch (Exception e) {
            appointmentNotificationService.updateNotificationStatus(
                    FAILED,
                    e.getMessage(),
                    null,
                    notification.getId()
            );
        }
    }

    public void sendAppointmentConfirmationMessage(Long appointmentId) {
        processNotification(appointmentId, CONFIRMATION);
    }

    @Transactional
    public void sendAppointmentReminderMessage(Long appointmentId) {
        processNotification(appointmentId, REMINDER);
    }

    private String buildMessage(Appointment appointment, AppointmentNotificationType type) {
        return switch (type) {
            case CONFIRMATION -> messageBuilder.buildAppointmentConfirmationMessage(appointment);
            case REMINDER -> messageBuilder.buildAppointmentReminderMessage(appointment);
        };
    }

    private SentMessageResult dispatchMessage(String tenantId,
                                              String message,
                                              String targetNumber) {
        return whatsappProvider.sendText(
                tenantId,
                message,
                targetNumber
        );
    }
}