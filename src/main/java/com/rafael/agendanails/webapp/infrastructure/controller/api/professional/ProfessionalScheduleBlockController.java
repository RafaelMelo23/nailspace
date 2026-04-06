package com.rafael.agendanails.webapp.infrastructure.controller.api.professional;

import com.rafael.agendanails.webapp.application.professional.ProfessionalScheduleBlockUseCase;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.config.SwaggerExamples;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockOutDTO;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/professional/schedule/block")
@Tag(name = "Professional - Schedule", description = "Professional schedule blocks")
public class ProfessionalScheduleBlockController {

    private final ProfessionalScheduleBlockUseCase professionalScheduleBlockUseCase;

    @Operation(summary = "Create schedule block", description = "Creates a schedule block for the professional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Block created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ScheduleBlockDTO.class),
                    examples = @ExampleObject(name = "ScheduleBlockRequest", value = SwaggerExamples.SCHEDULE_BLOCK_REQUEST))
    )
    @PostMapping
    public ResponseEntity<Void> createBlock(@Valid @RequestBody ScheduleBlockDTO blockDTO,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalScheduleBlockUseCase.createBlock(blockDTO, userPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Delete schedule block", description = "Deletes a schedule block.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Block deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid block id"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> deleteBlock(@Parameter(example = "7001")
                                            @PathVariable @Positive(message = "O identificador do bloqueio deve ser positivo") Long blockId,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalScheduleBlockUseCase.deleteBlock(blockId, userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get schedule blocks", description = "Returns schedule blocks for a given date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Blocks returned",
                    content = @Content(schema = @Schema(implementation = ScheduleBlockOutDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ResponseEntity<List<ScheduleBlockOutDTO>> getBlocks(@Parameter(example = "2026-04-01T00:00:00")
                                                               @RequestParam @NotNull(message = "A data e hora são obrigatórias")
                                                                   ZonedDateTime dateAndTime,
                                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ResponseEntity.ok(professionalScheduleBlockUseCase.getBlocks(userPrincipal, dateAndTime));
    }
}
