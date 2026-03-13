package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

import java.time.DayOfWeek;
import java.time.LocalTime;


@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "work_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_professional_day",
                        columnNames = {"professional_id", "day_of_week"}
                )
        }
)
@Filter(name = "tenantFilter",
        condition = "tenant_id = :tenantId"
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

    public void updateFromDto(WorkScheduleRecordDTO dto) {
        if (dto.dayOfWeek() != null) this.dayOfWeek = dto.dayOfWeek();
        if (dto.startTime() != null) this.workStart = dto.startTime();
        if (dto.endTime() != null) this.workEnd = dto.endTime();
        if (dto.lunchBreakStartTime() != null) this.lunchBreakStartTime = dto.lunchBreakStartTime();
        if (dto.lunchBreakEndTime() != null) this.lunchBreakEndTime = dto.lunchBreakEndTime();
        if (dto.isActive() != null) this.isActive = dto.isActive();
    }

    @Builder
    public WorkSchedule(DayOfWeek dayOfWeek, LocalTime workStart, LocalTime workEnd,
                        LocalTime lunchBreakStartTime, LocalTime lunchBreakEndTime,
                        Boolean isActive, Professional professional) {

        validateTimes(workStart, workEnd, dayOfWeek);

        this.dayOfWeek = dayOfWeek;
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.lunchBreakStartTime = lunchBreakStartTime;
        this.lunchBreakEndTime = lunchBreakEndTime;
        this.isActive = isActive;
        this.professional = professional;
    }

    private void validateTimes(LocalTime start, LocalTime end, DayOfWeek day) {
        if (end.isBefore(start)) {
            throw new BusinessException("Horário de término não pode ser menor que o de início na " + day);
        }
    }
}