package com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event;

public record AppointmentCancelledEvent(
        Long appointmentId,
        String tenantId,
        Long clientId
) {
}

