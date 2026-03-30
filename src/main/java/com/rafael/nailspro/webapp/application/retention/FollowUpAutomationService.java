package com.rafael.nailspro.webapp.application.retention;

import com.rafael.nailspro.webapp.domain.model.RetentionForecast;
import com.rafael.nailspro.webapp.domain.repository.RetentionForecastRepository;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.rafael.nailspro.webapp.domain.enums.appointment.RetentionStatus.PENDING;

@Component
@RequiredArgsConstructor
public class FollowUpAutomationService {

    private final VisitPredictionService visitPredictionService;
    private final RetentionForecastRepository repository;

    @IgnoreTenantFilter
    @Scheduled(cron = "0 30 9 * * *")
    public void sendMaintenanceForecastMessage() {
        Instant now = Instant.now();
        Instant twoDaysFromNow = now.plus(2, ChronoUnit.DAYS);

        List<RetentionForecast> forecasts =
                repository.findAllPredictedForecastsBetween(now, twoDaysFromNow, PENDING);

        forecasts.forEach(fr -> visitPredictionService.sendRetentionMaintenanceMessage(fr.getId()));
    }
}