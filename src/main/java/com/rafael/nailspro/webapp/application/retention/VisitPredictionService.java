package com.rafael.nailspro.webapp.application.retention;

import com.rafael.nailspro.webapp.application.messages.RetentionMessageBuilder;
import com.rafael.nailspro.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.model.AppointmentAddOn;
import com.rafael.nailspro.webapp.domain.model.RetentionForecast;
import com.rafael.nailspro.webapp.domain.repository.RetentionForecastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.rafael.nailspro.webapp.domain.enums.appointment.RetentionStatus.*;

@Service
@RequiredArgsConstructor
public class VisitPredictionService {

    private final RetentionForecastRepository repository;
    private final RetentionMessageBuilder messageBuilder;
    private final WhatsappProvider whatsappProvider;

    @Transactional
    public void createForecast(Appointment appointment) {
        List<RetentionForecast> forecastsToSave = new ArrayList<>();

        if (appointment.getMainSalonService().getMaintenanceIntervalDays() != null) {
            forecastsToSave.add(RetentionForecast.create(appointment, appointment.getMainSalonService()));
        }

        List<RetentionForecast> addOnRetentions = appointment.getAddOns().stream()
                .map(AppointmentAddOn::getService)
                .filter(ss -> ss.getMaintenanceIntervalDays() != null)
                .map(ss -> RetentionForecast.create(appointment, ss))
                .toList();

        forecastsToSave.addAll(addOnRetentions);

        if (!forecastsToSave.isEmpty()) {
            repository.saveAll(forecastsToSave);
        }
    }

    @Transactional
    public void markForecastAsExpired(RetentionForecast forecast) {
        if (forecast.getPredictedReturnDate().isAfter(Instant.now())) {
            throw new IllegalStateException("Can't expire an still active forecast");
        }

        forecast.setStatus(EXPIRED);
    }

    @Transactional
    public void markForecastAsConverted(RetentionForecast forecast) {

        forecast.setStatus(CONVERTED);
    }

    //todo: review + add deletion for converted or expireds
    @Async("messagingExecutor")
    public void sendMaintenanceMessage(Long retentionForecastId) {
        RetentionForecast retentionForecast = repository.findById(retentionForecastId)
                .orElseThrow(() -> new IllegalArgumentException("Can't find retention forecast with id: " + retentionForecastId));
        try {
            String message = messageBuilder.buildRetentionMessage(retentionForecast);

            whatsappProvider.sendText(
                    retentionForecast.getTenantId(),
                    message,
                    retentionForecast.getClient().getPhoneNumber()
            );

        } catch (Exception e) {
            retentionForecast.setStatus(FAILED_TO_SEND);
        }
    }
}
