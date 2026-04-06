package com.rafael.agendanails.webapp.infrastructure.controller.api.admin.professional;

import com.rafael.agendanails.webapp.application.professional.ProfessionalScheduleBlockUseCase;
import com.rafael.agendanails.webapp.application.professional.ProfessionalWorkScheduleUseCase;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockOutDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/professional")
@RequiredArgsConstructor
public class ProfessionalScheduleController {

    private final ProfessionalWorkScheduleUseCase professionalWorkScheduleUseCase;
    private final ProfessionalScheduleBlockUseCase professionalScheduleBlockUseCase;

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
            @Parameter(description = "ID of the professional", example = "2002")
            @PathVariable Long professionalId) {

        Set<WorkScheduleRecordDTO> schedules = professionalWorkScheduleUseCase.getWorkSchedules(professionalId);
        return ResponseEntity.ok(schedules);
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
            @Parameter(description = "ID of the professional", example = "2002")
            @PathVariable Long professionalId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Optional date to filter blocks", example = "2026-04-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime dateAndTime) {

        List<ScheduleBlockOutDTO> blocks = professionalScheduleBlockUseCase.getBlocks(
                professionalId,
                principal.getTenantId(),
                dateAndTime);
        return ResponseEntity.ok(blocks);
    }
}