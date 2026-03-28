package com.rafael.nailspro.webapp.application.retention;

import com.rafael.nailspro.webapp.domain.repository.RetentionForecastRepository;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionForecastDeletionJob {

    private final RetentionForecastRepository repository;

    @IgnoreTenantFilter
    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldForecasts() {
        Instant cutOffDate = Instant.now().minus(180, ChronoUnit.DAYS);
        try {
            int deleted = repository.deleteByPredictedReturnDateBefore(cutOffDate);
            if (deleted > 0) {
                log.info("Deleted {} retention forecasts older than {}", deleted, cutOffDate);
            }
        } catch (Exception ex) {
            log.error("Retention forecast deletion job failed", ex);
        }
    }
}