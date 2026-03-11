package com.rafael.nailspro.webapp.infrastructure.controller.api.professional;

import com.rafael.nailspro.webapp.application.professional.ProfessionalScheduleBlockUseCase;
import com.rafael.nailspro.webapp.domain.model.UserPrincipal;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockOutDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/professional/schedule/block")
public class ProfessionalScheduleBlockController {

    private final ProfessionalScheduleBlockUseCase professionalScheduleBlockUseCase;

    @PostMapping
    public ResponseEntity<Void> createBlock(@Valid @RequestBody ScheduleBlockDTO blockDTO,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalScheduleBlockUseCase.createBlock(blockDTO, userPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> deleteBlock(@PathVariable @Positive(message = "O identificador do bloqueio deve ser positivo") Long blockId,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalScheduleBlockUseCase.deleteBlock(userPrincipal.getUserId(), blockId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{dateAndTime}")
    public ResponseEntity<List<ScheduleBlockOutDTO>> getBlocks(@PathVariable @NotNull(message = "A data e hora são obrigatórias") LocalDateTime dateAndTime,
                                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ResponseEntity.ok(professionalScheduleBlockUseCase.getBlocks(userPrincipal.getUserId(), Optional.of(dateAndTime)));
    }
}
