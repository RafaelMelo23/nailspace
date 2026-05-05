package com.rafael.agendanails.webapp.application.appointment.message.schedule;

import com.rafael.agendanails.webapp.application.appointment.message.AppointmentMessagingUseCase;
import com.rafael.agendanails.webapp.domain.repository.WhatsappMessageRepository;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageStatus.FAILED;
import static com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageType.CONFIRMATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentConfirmationRetryableJob {

    private final AppointmentMessagingUseCase messagingUseCase;
    private final WhatsappMessageRepository messageRepository;
    // todo: consider changing to a circuit-breaker like approach for all scheduled sending message classes

    @IgnoreTenantFilter
    @Scheduled(cron = "0 */5 * * * *")
    public void retryFailedConfirmationMessages() {
        final int MAX_RETRIES = 3;
        try {
            List<Long> appointmentIds =
                    messageRepository.findRetriableAppointmentIds(MAX_RETRIES, FAILED, CONFIRMATION);

            appointmentIds.forEach(appointmentId ->
                    messagingUseCase.processNotification(appointmentId, CONFIRMATION));
        } catch (Exception e) {
            log.error("Failed to batch process failed confirmation messages");
        }
    }
}
