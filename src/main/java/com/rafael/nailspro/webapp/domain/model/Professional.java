package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
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

    public void prePersist() {
        this.isActive = Boolean.TRUE;
        this.isFirstLogin = Boolean.TRUE;
        this.externalId = UUID.randomUUID();
    }

    public void registerNewSchedules(List<WorkScheduleRecordDTO> dtos) {
        Set<DayOfWeek> existingDays = getExistingScheduleDays();

        dtos.stream()
                .filter(dto -> !existingDays.contains(dto.dayOfWeek()))
                .map(dto -> WorkSchedule.builder()
                        .dayOfWeek(dto.dayOfWeek())
                        .workStart(dto.startTime())
                        .workEnd(dto.endTime())
                        .lunchBreakStartTime(dto.lunchBreakStartTime())
                        .lunchBreakEndTime(dto.lunchBreakEndTime())
                        .isActive(true)
                        .professional(this)
                        .build())
                .forEach(this.workSchedules::add);
    }

    private Set<DayOfWeek> getExistingScheduleDays() {
        return this.workSchedules.stream()
                .map(WorkSchedule::getDayOfWeek)
                .collect(Collectors.toSet());
    }
}