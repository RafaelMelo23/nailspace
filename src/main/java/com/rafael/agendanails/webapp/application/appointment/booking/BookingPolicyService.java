package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.BookingPolicy;
import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.model.SalonService;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingPolicyService {

    private final AppointmentRepository appointmentRepository;
    private final SalonProfileService salonProfileService;
    private final BookingPolicy bookingPolicy;
    private final Clock clock;

    public void enforceBookingHorizon(LocalDateTime requestedTime, UserPrincipal userPrincipal) {
        SalonProfile profile = salonProfileService.getByTenantId(userPrincipal.getTenantId());
        if (!profile.isLoyalClientelePrioritized()) {
            return;
        }

        boolean isLoyalClient = isClientLoyal(userPrincipal.getId());

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

    private boolean isClientLoyal(Long clientId) {
        if (clientId == null) return false;

        return appointmentRepository
                .countByClientIdAndAppointmentStatus(
                        clientId,
                        AppointmentStatus.FINISHED
                ) >= 3;
    }

    public AppointmentTimeWindow calculateAllowedWindow(
            List<SalonService> services,
            String tenantId,
            Long userId
    ) {
        SalonProfile profile =
                salonProfileService.getByTenantId(tenantId);

        boolean isLoyalClient = isClientLoyal(userId);

        int windowDays = bookingPolicy.resolveAllowedWindowDays(
                profile.isLoyalClientelePrioritized(),
                isLoyalClient,
                profile.getLoyalClientBookingWindowDays(),
                profile.getStandardBookingWindow()
        );

        Appointment lastAppointment = userId != null ?
                appointmentRepository
                        .findFirstByClientIdAndAppointmentStatusOrderByStartDateDesc(userId, AppointmentStatus.FINISHED)
                        .orElse(null) : null;

        LocalDate startDate =
                bookingPolicy.determineStartDate(
                        services,
                        lastAppointment
                );

        return bookingPolicy.buildWindow(startDate, windowDays);
    }

    public Instant calculateEarliestRecommendedDate(Long clientId) {
        if (clientId == null) return null;

        Appointment appointment = appointmentRepository
                .findFirstByClientIdAndAppointmentStatusOrderByStartDateDesc(clientId, AppointmentStatus.FINISHED)
                .orElse(null);

        return bookingPolicy.calculateEarliestRecommendedDate(appointment);
    }
}