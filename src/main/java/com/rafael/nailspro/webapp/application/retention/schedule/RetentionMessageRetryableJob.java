package com.rafael.nailspro.webapp.application.retention.schedule;

import com.rafael.nailspro.webapp.application.retention.VisitPredictionService;
import com.rafael.nailspro.webapp.domain.model.WhatsappMessage;
import com.rafael.nailspro.webapp.domain.repository.WhatsappMessageRepository;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageStatus.FAILED;
import static com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageType.RETENTION_MAINTENANCE;

@Service
@RequiredArgsConstructor
public class RetentionMessageRetryableJob {

    private final VisitPredictionService visitPredictionService;
    private final WhatsappMessageRepository messageRepository;

    @IgnoreTenantFilter
    @Scheduled(cron = "0 */5 * * * *")
    public void retryFailedRetentionMessages() {
        final int MAX_RETRIES = 3;

        List<WhatsappMessage> messages =
                messageRepository.findRetriableMessages(MAX_RETRIES, FAILED, RETENTION_MAINTENANCE);

        messages.forEach(message -> {
            if (message.getRetentionForecast() == null) {
                return;
            }
            visitPredictionService.sendRetentionMaintenanceMessage(message.getRetentionForecast().getId());
        });
    }
}
