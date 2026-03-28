package com.rafael.nailspro.webapp.application.appointment;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.domain.BookingPolicy;
import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.domain.model.SalonService;
import com.rafael.nailspro.webapp.domain.model.UserPrincipal;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingPolicyService {

    private final AppointmentRepository appointmentRepository;
    private final SalonProfileService salonProfileService;
    private final BookingPolicy bookingPolicy;
    private final Clock clock;

    public void validate(LocalDateTime requestedTime, UserPrincipal userPrincipal) {
        SalonProfile profile = salonProfileService.getByTenantId(userPrincipal.getTenantId());
        if (!profile.isLoyalClientelePrioritized()) {
            return;
        }

        boolean isLoyalClient = isLoyalClient(userPrincipal.getUserId());

        int allowedDays = bookingPolicy.resolveAllowedWindowDays(
                profile.isLoyalClientelePrioritized(),
                isLoyalClient,
                profile.getLoyalClientBookingWindowDays(),
                profile.getStandardBookingWindow()
        );

        bookingPolicy.validateBookingHorizon(
                requestedTime.toLocalDate(),
                allowedDays,
                LocalDate.now(clock)
        );
    }

    public AppointmentTimeWindow calculateAllowedWindow(
            List<SalonService> services,
            UserPrincipal userPrincipal
    ) {
        SalonProfile profile =
                salonProfileService.getByTenantId(userPrincipal.getTenantId());

        boolean isLoyalClient = isLoyalClient(userPrincipal.getUserId());

        int windowDays = bookingPolicy.resolveAllowedWindowDays(
                profile.isLoyalClientelePrioritized(),
                isLoyalClient,
                profile.getLoyalClientBookingWindowDays(),
                profile.getStandardBookingWindow()
        );

        Appointment lastAppointment =
                appointmentRepository.findFirstByClientIdOrderByStartDateDesc(userPrincipal.getUserId())
                        .orElse(null);

        LocalDate startDate =
                bookingPolicy.determineStartDate(
                        services,
                        lastAppointment
                );

        return bookingPolicy.buildWindow(startDate, windowDays);
    }

    private boolean isLoyalClient(Long clientId) {
        if (clientId == null) return false;

        return appointmentRepository
                .countByClientIdAndAppointmentStatus(
                        clientId,
                        AppointmentStatus.FINISHED
                ) >= 3;
    }

    public Instant calculateEarliestRecommendedDate(Long clientId) {
        Appointment appointment = appointmentRepository
                .findFirstByClientIdOrderByStartDateDesc(clientId)
                .orElse(null);

        return bookingPolicy.calculateEarliestRecommendedDate(appointment);
    }
}