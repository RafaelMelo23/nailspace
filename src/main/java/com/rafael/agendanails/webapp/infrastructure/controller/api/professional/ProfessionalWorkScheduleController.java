package com.rafael.agendanails.webapp.infrastructure.controller.api.professional;

import com.rafael.agendanails.webapp.application.professional.ProfessionalWorkScheduleUseCase;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.domain.model.WorkSchedule;
import com.rafael.agendanails.webapp.infrastructure.config.SwaggerExamples;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/professional/schedule")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Professional - Schedule", description = "Professional work schedule")
public class ProfessionalWorkScheduleController {

    private final ProfessionalWorkScheduleUseCase professionalWorkScheduleUseCase;

    @Operation(summary = "Get work schedules", description = "Returns the professional's work schedules.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedules returned",
                    content = @Content(schema = @Schema(implementation = WorkScheduleRecordDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ResponseEntity<Set<WorkScheduleRecordDTO>> getSchedules(@AuthenticationPrincipal
                                                                       UserPrincipal userPrincipal) {

        return ResponseEntity.ok(professionalWorkScheduleUseCase.getWorkSchedules(userPrincipal.getId()));
    }

    @Operation(summary = "Create work schedules", description = "Creates work schedules for the professional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Schedules created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkScheduleRecordDTO.class),
                    examples = @ExampleObject(name = "WorkScheduleListRequest", value = SwaggerExamples.WORK_SCHEDULE_LIST_REQUEST))
    )
    @PostMapping
    public ResponseEntity<List<WorkSchedule>> createWorkSchedule(@Valid @RequestBody List<WorkScheduleRecordDTO> workScheduleRecordDTO,
                                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ResponseEntity.ok(professionalWorkScheduleUseCase.createSchedules(workScheduleRecordDTO, userPrincipal.getId()));
    }

    @Operation(summary = "Modify work schedules", description = "Modifies the professional's weekly schedule.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Schedules updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkScheduleRecordDTO.class),
                    examples = @ExampleObject(name = "WorkScheduleListRequest", value = SwaggerExamples.WORK_SCHEDULE_LIST_REQUEST))
    )
    @PatchMapping
    public ResponseEntity<Void> modifySchedules(@Valid @RequestBody List<WorkScheduleRecordDTO> workScheduleRecordDTO,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalWorkScheduleUseCase.modifyWeekSchedule(workScheduleRecordDTO, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete work schedule", description = "Deletes a work schedule by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Schedule deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid schedule id"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
                                               @io.swagger.v3.oas.annotations.Parameter(example = "8001")
                                               @PathVariable Long scheduleId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalWorkScheduleUseCase.deleteSchedule(scheduleId, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}
