package com.rafael.agendanails.webapp.application.appointment.message;

import com.rafael.agendanails.webapp.application.messages.AppointmentMessageBuilder;
import com.rafael.agendanails.webapp.application.whatsapp.message.WhatsappMessageService;
import com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageStatus;
import com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageType;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.domain.whatsapp.SentMessageResult;
import com.rafael.agendanails.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageStatus.FAILED;
import static com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageType.CONFIRMATION;
import static com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageType.REMINDER;

@Log4j2
@Service
@RequiredArgsConstructor
public class AppointmentMessagingUseCase {

    private final WhatsappProvider whatsappProvider;
    private final AppointmentMessageBuilder messageBuilder;
    private final WhatsappMessageService whatsappMessageService;
    private final AppointmentRepository appointmentRepository;

    @IgnoreTenantFilter
    public void processNotification(Long appointmentId, WhatsappMessageType type) {
        var message = whatsappMessageService.prepareAppointmentMessage(appointmentId, type);

        try {
            var appointment = appointmentRepository.findFullById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            var messageBody = buildMessage(appointment, type);

            SentMessageResult sentMessageResult = dispatchMessage(
                    appointment.getTenantId(),
                    messageBody,
                    message.getDestinationNumber()
            );

            whatsappMessageService.updateMessageStatus(
                    WhatsappMessageStatus.fromEvolutionStatus(sentMessageResult.status()),
                    null,
                    sentMessageResult.messageId(),
                    message.getId()
            );
        } catch (Exception e) {
            whatsappMessageService.updateMessageStatus(
                    FAILED,
                    e.getMessage(),
                    null,
                    message.getId()
            );
        }
    }

    @IgnoreTenantFilter
    public void sendAppointmentConfirmationMessage(Long appointmentId) {
        processNotification(appointmentId, CONFIRMATION);
    }

    @IgnoreTenantFilter
    @Transactional
    public void sendAppointmentReminderMessage(Long appointmentId) {
        processNotification(appointmentId, REMINDER);
    }

    private String buildMessage(Appointment appointment, WhatsappMessageType type) {
        return switch (type) {
            case CONFIRMATION -> messageBuilder.buildAppointmentConfirmationMessage(appointment);
            case REMINDER -> messageBuilder.buildAppointmentReminderMessage(appointment);
            default -> throw new IllegalArgumentException("Unsupported appointment message type: " + type);
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
