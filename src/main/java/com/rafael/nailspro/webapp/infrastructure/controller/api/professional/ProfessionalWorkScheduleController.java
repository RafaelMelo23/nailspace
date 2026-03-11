package com.rafael.nailspro.webapp.infrastructure.controller.api.professional;

import com.rafael.nailspro.webapp.application.professional.ProfessionalWorkScheduleUseCase;
import com.rafael.nailspro.webapp.domain.model.UserPrincipal;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/professional/schedule")
public class ProfessionalWorkScheduleController {

    private final ProfessionalWorkScheduleUseCase professionalWorkScheduleUseCase;

    @GetMapping
    public ResponseEntity<Set<WorkScheduleRecordDTO>> getSchedules(@AuthenticationPrincipal
                                                                       UserPrincipal userPrincipal) {

        return ResponseEntity.ok(professionalWorkScheduleUseCase.getWorkSchedules(userPrincipal.getUserId()));
    }

    @PostMapping
    public ResponseEntity<Void> createWorkSchedule(@Valid @RequestBody List<WorkScheduleRecordDTO> workScheduleRecordDTO,
                                                   @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalWorkScheduleUseCase.registerSchedules(workScheduleRecordDTO, userPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    public ResponseEntity<Void> modifySchedules(@Valid @RequestBody List<WorkScheduleRecordDTO> workScheduleRecordDTO,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalWorkScheduleUseCase.modifyWeekSchedule(workScheduleRecordDTO, userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {

        professionalWorkScheduleUseCase.deleteSchedule(scheduleId, userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
