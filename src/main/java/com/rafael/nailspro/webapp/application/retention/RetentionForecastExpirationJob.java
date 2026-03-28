package com.rafael.nailspro.webapp.application.retention;

import com.rafael.nailspro.webapp.domain.enums.appointment.RetentionStatus;
import com.rafael.nailspro.webapp.domain.model.RetentionForecast;
import com.rafael.nailspro.webapp.domain.repository.RetentionForecastRepository;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionForecastExpirationJob {

    private final VisitPredictionService visitPredictionService;
    private final RetentionForecastRepository repository;

    @IgnoreTenantFilter
    @Scheduled(cron = "0 0 0 * * *")
    public void expireForecasts() {
        Instant now = Instant.now();
        try {
            List<RetentionForecast> expiredForecasts =
                    repository.findAllExpiredPredictedForecastsByStatus(
                            now, List.of(RetentionStatus.PENDING, RetentionStatus.NOTIFIED)
                    );

            int processed = 0;
            for (RetentionForecast forecast : expiredForecasts) {
                try {
                    visitPredictionService.markForecastAsExpired(forecast);
                    processed++;
                } catch (Exception ex) {
                    log.error("Failed to expire retention forecast id={}", forecast.getId(), ex);
                }
            }
            if (processed > 0) {
                log.info(
                        "Retention forecast expiration job: expired={} referenceTime={}",
                        processed, now
                );
            }
        } catch (Exception ex) {
            log.error("Retention forecast expiration job failed", ex);
        }
    }
}