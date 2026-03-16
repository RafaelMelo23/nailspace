package com.rafael.nailspro.webapp.infrastructure.controller.api.admin;

import com.rafael.nailspro.webapp.application.admin.professional.ProfessionalManagementService;
import com.rafael.nailspro.webapp.application.professional.ProfessionalQueryService;
import com.rafael.nailspro.webapp.application.professional.ProfessionalScheduleBlockUseCase;
import com.rafael.nailspro.webapp.application.professional.ProfessionalWorkScheduleUseCase;
import com.rafael.nailspro.webapp.domain.model.UserPrincipal;
import com.rafael.nailspro.webapp.infrastructure.config.SwaggerExamples;
import com.rafael.nailspro.webapp.infrastructure.dto.admin.professional.CreateProfessionalDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.ProfessionalResponseDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockOutDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/professional")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Professionals", description = "Professional management")
public class ProfessionalManagementController {

    private final ProfessionalManagementService managementService;
    private final ProfessionalQueryService queryService;
    private final ProfessionalWorkScheduleUseCase professionalWorkScheduleUseCase;
    private final ProfessionalScheduleBlockUseCase professionalScheduleBlockUseCase;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create professional", description = "Creates a new professional user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Professional created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CreateProfessionalDTO.class),
                    examples = @ExampleObject(name = "CreateProfessionalRequest", value = SwaggerExamples.CREATE_PROFESSIONAL_REQUEST))
    )
    @PostMapping
    public ResponseEntity<Void> createProfessional(
            @Valid @RequestBody CreateProfessionalDTO professionalDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        managementService.createProfessional(professionalDTO, userPrincipal.getTenantId());
        return ResponseEntity.status(201).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate professional", description = "Deactivates a professional by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Professional deactivated"),
            @ApiResponse(responseCode = "400", description = "Invalid professional id"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateProfessional(
            @Parameter(example = "2002")
            @PathVariable Long id) {

        managementService.deactivateProfessional(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List professionalsIds", description = "Returns all professionalsIds.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professionals returned",
                    content = @Content(schema = @Schema(implementation = ProfessionalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ResponseEntity<List<ProfessionalResponseDTO>> getAllProfessionals() {

        return ResponseEntity.ok(queryService.findAllProfessionals());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get professional work schedule", description = "Returns the work schedule of a professional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work schedule returned",
                    content = @Content(schema = @Schema(implementation = WorkScheduleRecordDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid professional id"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/schedule/{professionalId}")
    public ResponseEntity<Set<WorkScheduleRecordDTO>> getProfessionalWorkSchedule(
            @Parameter(example = "2002")
            @PathVariable Long professionalId) {

        return ResponseEntity.ok(professionalWorkScheduleUseCase.getWorkSchedules(professionalId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get schedule blocks", description = "Returns schedule blocks for a professional in a date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule blocks returned",
                    content = @Content(schema = @Schema(implementation = ScheduleBlockOutDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid professional id or date"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/schedule/block/{professionalId}")
    public ResponseEntity<List<ScheduleBlockOutDTO>> getProfessionalScheduleBlocks(
            @Parameter(example = "2002")
            @PathVariable Long professionalId,
            @Parameter(example = "2026-04-01T00:00:00")
            @RequestParam LocalDateTime dateAndTime) {

        return ResponseEntity.ok(professionalScheduleBlockUseCase.getBlocks(professionalId, Optional.of(dateAndTime)));
    }
}

