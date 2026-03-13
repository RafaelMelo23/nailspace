package com.rafael.nailspro.webapp.domain;

import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.model.AppointmentAddOn;
import com.rafael.nailspro.webapp.domain.model.SalonService;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class BookingPolicy {

    public int resolveAllowedWindowDays(
            Boolean prioritizeLoyalClients,
            Boolean isLoyalClient,
            Integer loyalClientWindowDays,
            Integer standardWindowDays
    ) {
        boolean prioritize = Boolean.TRUE.equals(prioritizeLoyalClients);
        boolean loyal = Boolean.TRUE.equals(isLoyalClient);

        int standardDays = standardWindowDays != null ? standardWindowDays : 7;
        int loyalDays = loyalClientWindowDays != null ? loyalClientWindowDays : 30;

        int resolved = !prioritize
                ? standardDays
                : (loyal ? loyalDays : standardDays);

        log.debug("Resolved booking window days -> prioritize: {}, loyal: {}, result: {}", prioritize, loyal, resolved);
        return resolved;
    }

    public void validateBookingHorizon(
            LocalDate requestedDate,
            int allowedDays,
            LocalDate today
    ) {
        long daysAhead = ChronoUnit.DAYS.between(today, requestedDate);

        log.debug(
                "Validating booking horizon -> requestedDate: {}, today: {}, daysAhead: {}, allowedDays: {}",
                requestedDate,
                today,
                daysAhead,
                allowedDays
        );

        if (daysAhead > allowedDays) {

            log.warn(
                    "Booking rejected due to horizon policy -> requestedDate: {}, daysAhead: {}, allowedDays: {}",
                    requestedDate,
                    daysAhead,
                    allowedDays
            );

            throw new BusinessException(
                    "Sua janela de agendamento não é preferencial, aguarde alguns dias para reservar esta data."
            );
        }
    }

    public AppointmentTimeWindow buildWindow(
            LocalDate startDate,
            int windowDays
    ) {

        AppointmentTimeWindow window = AppointmentTimeWindow.builder()
                .start(startDate)
                .end(startDate.plusDays(windowDays))
                .build();

        log.debug(
                "Booking window built -> start: {}, end: {}",
                window.start(),
                window.end()
        );

        return window;
    }

    public LocalDate determineStartDate(
            List<SalonService> services,
            Optional<Appointment> lastAppointment,
            LocalDate today
    ) {

        Integer maintenanceInterval = services.stream()
                .map(SalonService::getMaintenanceIntervalDays)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);

        log.debug(
                "Determining start date -> maintenanceInterval: {}, hasLastAppointment: {}",
                maintenanceInterval,
                lastAppointment.isPresent()
        );

        if (maintenanceInterval == null || lastAppointment.isEmpty()) {

            log.debug("No maintenance restriction applied. Using today: {}", today);

            return today;
        }

        Appointment last = lastAppointment.get();
        ZoneId zoneId = last.getSalonZoneId();

        LocalDate calculated = LocalDate
                .ofInstant(last.getStartDate(), zoneId)
                .plusDays(maintenanceInterval - 3);

        log.debug(
                "Start date determined from maintenance interval -> lastAppointment: {}, zone: {}, result: {}",
                last.getStartDate(),
                zoneId,
                calculated
        );

        return calculated;
    }

    public Instant calculateEarliestRecommendedDate(Optional<Appointment> lastAppointment) {

        if (lastAppointment.isEmpty()) {

            log.debug("No last appointment found. Returning current instant.");

            return Instant.now();
        }

        Appointment last = lastAppointment.get();

        List<SalonService> allServices =
                Stream.concat(
                        Stream.of(last.getMainSalonService()),
                        last.getAddOns().stream().map(AppointmentAddOn::getService)
                ).toList();

        Integer maxInterval = allServices.stream()
                .map(SalonService::getMaintenanceIntervalDays)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        Instant recommended = last.getStartDate().plus(maxInterval, ChronoUnit.DAYS);

        log.debug(
                "Earliest recommended booking date calculated -> lastAppointment: {}, maxIntervalDays: {}, result: {}",
                last.getStartDate(),
                maxInterval,
                recommended
        );

        return recommended;
    }
}