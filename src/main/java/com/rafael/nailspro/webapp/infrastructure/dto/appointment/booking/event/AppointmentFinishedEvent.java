package com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record AppointmentFinishedEvent(Long appointmentId,
                                       Long clientId,
                                       String tenantId,
                                       BigDecimal totalValue,
                                       ZonedDateTime completionDate) {
}
