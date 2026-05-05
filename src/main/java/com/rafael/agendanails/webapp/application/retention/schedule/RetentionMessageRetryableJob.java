package com.rafael.agendanails.webapp.application.retention.schedule;

import com.rafael.agendanails.webapp.application.retention.VisitPredictionService;
import com.rafael.agendanails.webapp.domain.repository.WhatsappMessageRepository;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageStatus.FAILED;
import static com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageType.RETENTION_MAINTENANCE;

@Service
@RequiredArgsConstructor
public class RetentionMessageRetryableJob {

    private final VisitPredictionService visitPredictionService;
    private final WhatsappMessageRepository messageRepository;

    @IgnoreTenantFilter
    @Scheduled(cron = "0 */5 * * * *")
    public void retryFailedRetentionMessages() {
        final int MAX_RETRIES = 3;

        List<Long> retentionForecastIds =
                messageRepository.findRetriableRetentionForecastIds(MAX_RETRIES, FAILED, RETENTION_MAINTENANCE);

        retentionForecastIds.forEach(visitPredictionService::sendRetentionMaintenanceMessage);
    }
}
