package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "work_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_professional_day",
                        columnNames = {"professional_id", "day_of_week"}
                )
        }
)
public class WorkSchedule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime workStart;

    @Column(name = "end_time", nullable = false)
    private LocalTime workEnd;

    @Column(name = "lunch_break_start_time", nullable = false)
    private LocalTime lunchBreakStartTime;

    @Column(name = "lunch_break_end_time", nullable = false)
    private LocalTime lunchBreakEndTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Builder(builderMethodName = "testBuilder")
    public WorkSchedule(Long id, DayOfWeek dayOfWeek, LocalTime workStart, LocalTime workEnd, LocalTime lunchBreakStartTime, LocalTime lunchBreakEndTime, Boolean isActive, Professional professional, String tenantId) {
        super(tenantId);
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.lunchBreakStartTime = lunchBreakStartTime;
        this.lunchBreakEndTime = lunchBreakEndTime;
        this.isActive = isActive;
        this.professional = professional;
    }

    public void assignProfessional(Professional professional) {
        this.professional = professional;
    }

    public void assignId(Long id) {
        this.id = id;
    }

    @Override
    public void prePersist() {
        if (this.professional != null && getTenantId() == null) {
            setTenantId(this.professional.getTenantId());
        }
    }

    public void updateSchedule(LocalTime workStart, LocalTime workEnd, LocalTime lunchBreakStartTime, LocalTime lunchBreakEndTime, Boolean isActive) {
        if (workStart != null) this.workStart = workStart;
        if (workEnd != null) {
            validateTimes(this.workStart, workEnd, this.dayOfWeek);
            this.workEnd = workEnd;
        }
        if (lunchBreakStartTime != null) this.lunchBreakStartTime = lunchBreakStartTime;
        if (lunchBreakEndTime != null) this.lunchBreakEndTime = lunchBreakEndTime;
        if (isActive != null) this.isActive = isActive;
    }

    public void updateFromDto(WorkScheduleRecordDTO dto) {
        if (dto.dayOfWeek() != null) this.dayOfWeek = dto.dayOfWeek();
        if (dto.startTime() != null) this.workStart = dto.startTime();
        if (dto.endTime() != null) {
            validateTimes(this.workStart, dto.endTime(), this.dayOfWeek);
            this.workEnd = dto.endTime();
        }
        if (dto.lunchBreakStartTime() != null) this.lunchBreakStartTime = dto.lunchBreakStartTime();
        if (dto.lunchBreakEndTime() != null) this.lunchBreakEndTime = dto.lunchBreakEndTime();
        if (dto.isActive() != null) this.isActive = dto.isActive();
    }

    public WorkSchedule(DayOfWeek dayOfWeek,
                        LocalTime workStart,
                        LocalTime workEnd,
                        LocalTime lunchBreakStartTime,
                        LocalTime lunchBreakEndTime,
                        Boolean isActive,
                        Professional professional) {

        validateTimes(workStart, workEnd, dayOfWeek);

        this.dayOfWeek = dayOfWeek;
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.lunchBreakStartTime = lunchBreakStartTime;
        this.lunchBreakEndTime = lunchBreakEndTime;
        this.isActive = isActive;
        this.professional = professional;
        this.setTenantId(professional.getTenantId());
    }

    private void validateTimes(LocalTime start, LocalTime end, DayOfWeek day) {
        if (end.isBefore(start)) {
            throw new BusinessException("Horário de término não pode ser menor que o de início na " + day);
        }
    }

    public static Set<WorkSchedule> createDefaultWeek(Professional professional) {
        Set<DayOfWeek> days = Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );

        return days.stream()
                .map(day -> new WorkSchedule(
                        day,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        LocalTime.of(12, 0),
                        LocalTime.of(13, 0),
                        true,
                        professional
                ))
                .collect(Collectors.toSet());
    }
}