package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import lombok.Builder;

@Builder
public record AvailabilityQuery(
        Professional professional,
        AppointmentTimeWindow window,
        SalonProfile salonProfile,
        int serviceDurationInSeconds
) {
}
