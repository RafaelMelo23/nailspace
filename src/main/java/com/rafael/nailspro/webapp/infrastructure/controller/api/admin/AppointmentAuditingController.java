package com.rafael.nailspro.webapp.infrastructure.controller.api.admin;

import com.rafael.nailspro.webapp.application.admin.salon.profile.AppointmentAuditService;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AdminUserAppointmentDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/appointments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Appointments", description = "Appointment auditing for admins")
public class AppointmentAuditingController {

    private final AppointmentAuditService service;

    @Operation(summary = "List user appointments", description = "Lists appointments for a user with pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointments returned",
                    content = @Content(schema = @Schema(implementation = AdminUserAppointmentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid user id"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<AdminUserAppointmentDTO>> listUserAppointments(
            @Parameter(example = "2001")
            @PathVariable @Positive(message = "O identificador do usuário deve ser positivo") Long userId,
            @PageableDefault(
                    sort = "startDate"
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                service.listUsersAppointments(userId, pageable)
        );
    }
}
