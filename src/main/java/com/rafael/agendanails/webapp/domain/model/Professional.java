package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@SuperBuilder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Professional extends User {

    private String professionalPicture;

    @Column
    private UUID externalId = UUID.randomUUID();

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "is_first_login")
    private Boolean isFirstLogin = false;

    @OneToMany(mappedBy = "professional")
    private List<Appointment> professionalAppointments;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "professionals")
    private Set<SalonService> salonServices = new LinkedHashSet<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "professional", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<WorkSchedule> workSchedules = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "professional", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<ScheduleBlock> scheduleBlocks = new LinkedHashSet<>();

    @OneToOne(mappedBy = "owner", orphanRemoval = true)
    private SalonProfile salonProfile;

    @Override
    public void prePersist() {
        this.isActive = Boolean.TRUE;
        this.isFirstLogin = Boolean.TRUE;
        this.externalId = UUID.randomUUID();
    }

    public Set<WorkSchedule> registerNewSchedules(List<WorkScheduleRecordDTO> dtos) {
        validateAndCheckOverlap(dtos);

        Set<WorkSchedule> newSchedules = dtos.stream()
                .map(this::mapToEntity)
                .collect(Collectors.toSet());

        this.workSchedules.addAll(newSchedules);
        return newSchedules;
    }

    private void validateAndCheckOverlap(List<WorkScheduleRecordDTO> dtos) {
        Set<DayOfWeek> days = new HashSet<>();
        for(WorkScheduleRecordDTO dto : dtos) {
            validateDto(dto);
            checkForRepeatedDays(dto);

            if (!days.add(dto.dayOfWeek()))
                throw new BusinessException("A requisição contém dias duplicados.");
            if (!dto.startTime().isBefore(dto.endTime()))
                throw new BusinessException("O horário de início deve ser menor que o de término.");
            if (dto.lunchBreakStartTime().isAfter(dto.endTime()) || dto.lunchBreakEndTime().isAfter(dto.endTime()))
                throw new BusinessException("O horário de almoço não pode ser depois do término do expediente.");
        }
    }

    private void checkForRepeatedDays(WorkScheduleRecordDTO dto) {
        Set<DayOfWeek> existingDays = getExistingScheduleDays();
        if (existingDays.contains(dto.dayOfWeek())) throw new BusinessException("Horário já cadastrado para: " + dto.dayOfWeek());
    }

    private void validateDto(WorkScheduleRecordDTO dto) {
        if (dto.dayOfWeek() == null || dto.startTime() == null || dto.endTime() == null ||
                dto.lunchBreakStartTime() == null || dto.lunchBreakEndTime() == null) {
            throw new BusinessException("Todos os campos de horário são obrigatórios.");
        }
    }

    private WorkSchedule mapToEntity(WorkScheduleRecordDTO dto) {
        return WorkSchedule.builder()
                .dayOfWeek(dto.dayOfWeek())
                .workStart(dto.startTime())
                .workEnd(dto.endTime())
                .lunchBreakStartTime(dto.lunchBreakStartTime())
                .lunchBreakEndTime(dto.lunchBreakEndTime())
                .isActive(dto.isActive() != null ? dto.isActive() : true)
                .professional(this)
                .build();
    }

    private Set<DayOfWeek> getExistingScheduleDays() {
        return this.workSchedules.stream()
                .map(WorkSchedule::getDayOfWeek)
                .collect(Collectors.toSet());
    }
}