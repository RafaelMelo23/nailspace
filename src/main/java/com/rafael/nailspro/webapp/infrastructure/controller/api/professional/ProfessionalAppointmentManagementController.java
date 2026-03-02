package com.rafael.nailspro.webapp.infrastructure.controller.api.professional;

import com.rafael.nailspro.webapp.application.professional.ProfessionalAppointmentStatusUseCase;
import com.rafael.nailspro.webapp.application.professional.ProfessionalScheduleQueryUseCase;
import com.rafael.nailspro.webapp.domain.model.UserPrincipal;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.ProfessionalAppointmentScheduleDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESSIONAL')")
@RequestMapping("/api/v1/professional/appointments")
public class ProfessionalAppointmentManagementController {

    private final ProfessionalScheduleQueryUseCase professionalScheduleQueryUseCase;
    private final ProfessionalAppointmentStatusUseCase professionalAppointmentStatusUseCase;

    @GetMapping
    public ResponseEntity<List<ProfessionalAppointmentScheduleDTO>> findByDay(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @NotNull(message = "A data e hora inicial são obrigatórias")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
            @RequestParam @NotNull(message = "A data e hora final são obrigatórias")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end
    ) {
        return ResponseEntity.ok(
                professionalScheduleQueryUseCase.findProfessionalAppointmentsByDay(
                        principal.getUserId(),
                        start,
                        end
                )
        );
    }

    @PatchMapping("/{appointmentId}/confirm")
    public ResponseEntity<Void> confirmAppointment(
            @PathVariable @Positive(message = "O identificador do agendamento deve ser positivo") Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        professionalAppointmentStatusUseCase.confirm(appointmentId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{appointmentId}/finish")
    public ResponseEntity<Void> finishAppointment(
            @PathVariable @Positive(message = "O identificador do agendamento deve ser positivo") Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        professionalAppointmentStatusUseCase.finish(appointmentId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{appointmentId}/cancel")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable @Positive(message = "O identificador do agendamento deve ser positivo") Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        professionalAppointmentStatusUseCase.cancel(appointmentId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{appointmentId}/miss")
    public ResponseEntity<Void> missedAppointment(
            @PathVariable @Positive(message = "O identificador do agendamento deve ser positivo") Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        professionalAppointmentStatusUseCase.miss(appointmentId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}