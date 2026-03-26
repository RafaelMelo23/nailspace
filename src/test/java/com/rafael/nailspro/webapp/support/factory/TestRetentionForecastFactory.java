package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.appointment.RetentionStatus;
import com.rafael.nailspro.webapp.domain.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestRetentionForecastFactory {

    private static RetentionForecast.RetentionForecastBuilder<?, ?> baseBuilder() {
        return RetentionForecast.builder()
                .id(null)
                .client(null)
                .professional(TestProfessionalFactory.standard())
                .status(RetentionStatus.PENDING)
                .predictedReturnDate(Instant.now().plus(15, ChronoUnit.DAYS));
    }

    public static RetentionForecast standard() {
        return baseBuilder().build();
    }

    public static RetentionForecast standardForIt(Professional p,
                                                  Client c,
                                                  List<SalonService> s) {
        return baseBuilder()
                .client(c)
                .professional(p)
                .salonServices(s)
                .build();
    }

    public static RetentionForecast complete(Professional p,
                                                    Client c,
                                                    List<SalonService> s,
                                                    Appointment origin,
                                                    Instant returnDate) {
        return baseBuilder()
                .client(c)
                .professional(p)
                .salonServices(s)
                .originAppointment(origin)
                .predictedReturnDate(returnDate)
                .build();
    }

    public static RetentionForecast standardEnglish(Professional p,
                                             Client c,
                                             List<SalonService> s,
                                             Appointment origin,
                                             Instant returnDate) {
        return complete(p, c, s, origin, returnDate);
    }

    public static RetentionForecast withAppointmentAndStatus(Professional p,
                                                          Client c,
                                                          List<SalonService> s,
                                                          Appointment origin,
                                                          Instant returnDate,
                                                             RetentionStatus status) {
        return baseBuilder()
                .client(c)
                .professional(p)
                .salonServices(s)
                .originAppointment(origin)
                .status(status)
                .predictedReturnDate(returnDate)
                .build();
    }

    public static RetentionForecast expiredForIt(Professional p,
                                                 Client c,
                                                 List<SalonService> s) {
        long randomDays = ThreadLocalRandom.current().nextLong(1, 61);

        return baseBuilder()
                .predictedReturnDate(Instant.now().minus(randomDays, ChronoUnit.DAYS))
                .status(RetentionStatus.PENDING)
                .professional(p)
                .client(c)
                .salonServices(s)
                .build();
    }

    public static RetentionForecast activeWithOldReturnDate(Professional p,
                                                Client c,
                                                List<SalonService> s) {
        long randomDays = ThreadLocalRandom.current().nextLong(1, 61);

        return baseBuilder()
                .status(RetentionStatus.PENDING)
                .predictedReturnDate(Instant.now().minus(randomDays, ChronoUnit.DAYS))
                .professional(p)
                .client(c)
                .salonServices(s)
                .build();
    }

    public static RetentionForecast activeForIt(Professional p,
                                                Client c,
                                                List<SalonService> s) {
        return baseBuilder()
                .status(RetentionStatus.PENDING)
                .predictedReturnDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .professional(p)
                .client(c)
                .salonServices(s)
                .build();
    }

    public static RetentionForecast withId(Long id) {
        RetentionForecast forecast = standard();
        forecast.setId(id);
        return forecast;
    }
}