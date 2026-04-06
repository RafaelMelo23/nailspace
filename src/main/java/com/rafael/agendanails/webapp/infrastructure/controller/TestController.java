package com.rafael.agendanails.webapp.infrastructure.controller;

import com.rafael.agendanails.webapp.application.appointment.message.schedule.AppointmentReminderJob;
import com.rafael.agendanails.webapp.application.retention.VisitPredictionService;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.event.AppointmentConfirmedEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Test", description = "Test endpoints")
public class TestController {

    private final ApplicationEventPublisher publisher;
    private final AppointmentReminderJob job;
    private final VisitPredictionService visitPredictionService;

    @Operation(summary = "Trigger appointment confirmed", description = "Publishes an AppointmentConfirmedEvent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event published")
    })
    @PostMapping("/appointment-confirmed/{id}")
    @Transactional
    public void trigger(@Parameter(example = "3001") @PathVariable Long id) {
        publisher.publishEvent(new AppointmentConfirmedEvent(id));
    }

    @Operation(summary = "Trigger reminder job", description = "Runs the appointment reminder job.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job triggered")
    })
    @PostMapping("/trigger-reminder/{id}")
    @Transactional
    public void triggerReminder(@Parameter(example = "3001") @PathVariable Long id) {
        job.scheduleReminders();
    }

    @PostMapping("/trigger-retention-forecast/{id}")
    public void triggerRetentionForecast(@Parameter(example = "3001") @PathVariable Long id) {

        visitPredictionService.sendRetentionMaintenanceMessage(id);
    }
}