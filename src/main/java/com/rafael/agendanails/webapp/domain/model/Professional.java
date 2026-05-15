package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@Filter(name = "deletedFilter")
public class Professional extends User {

    @Builder.Default
    private String professionalPicture = null;

    @Builder.Default
    @Column
    private UUID externalId = UUID.randomUUID();

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = false;

    @Builder.Default
    @Column(name = "is_first_login")
    private Boolean isFirstLogin = false;

    @Builder.Default
    @OneToMany(mappedBy = "professional")
    private List<Appointment> professionalAppointments = new ArrayList<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "professionals")
    private Set<SalonService> salonServices = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "professional", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<WorkSchedule> workSchedules = new HashSet<>();

    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "professional", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<ScheduleBlock> scheduleBlocks = new LinkedHashSet<>();

    @OneToOne(mappedBy = "owner", orphanRemoval = true)
    private SalonProfile salonProfile;

    private Professional(String name, String email, UserRole role) {
        super(name, email, null, UserStatus.ACTIVE, role);
        this.isActive = true;
        this.isFirstLogin = true;
        this.externalId = UUID.randomUUID();
        this.workSchedules = new HashSet<>();
        this.scheduleBlocks = new LinkedHashSet<>();
        this.salonServices = new LinkedHashSet<>();
        this.professionalAppointments = new ArrayList<>();
    }

    @Override
    public void prePersist() {
        super.prePersist();
        if (this.isActive == null) this.isActive = Boolean.TRUE;
        if (this.isFirstLogin == null) this.isFirstLogin = true;
        if (this.externalId == null) this.externalId = UUID.randomUUID();
    }

    public static Professional createAdminProfessional(
            String name,
            String email
    ) {
        return new Professional(name, email, UserRole.ADMIN);
    }

    public void updatePicture(String picturePath) {
        this.professionalPicture = picturePath;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void markFirstLoginDone() {
        this.isFirstLogin = false;
    }

    public void forceFirstLoginStatus(boolean status) {
        this.isFirstLogin = status;
    }

    public void assignSalonProfile(SalonProfile salonProfile) {
        this.salonProfile = salonProfile;
    }

    public void assignWorkSchedules(Set<WorkSchedule> schedules) {
        this.workSchedules.clear();
        if (schedules != null) {
            this.workSchedules.addAll(schedules);
        }
    }

    public void assignSalonServices(Set<SalonService> services) {
        this.salonServices.clear();
        if (services != null) {
            this.salonServices.addAll(services);
        }
    }

    public void assignScheduleBlocks(Set<ScheduleBlock> blocks) {
        this.scheduleBlocks.clear();
        if (blocks != null) {
            this.scheduleBlocks.addAll(blocks);
        }
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
        return new WorkSchedule(
                dto.dayOfWeek(),
                dto.startTime(),
                dto.endTime(),
                dto.lunchBreakStartTime(),
                dto.lunchBreakEndTime(),
                dto.isActive() != null ? dto.isActive() : true,
                this
        );
    }

    private Set<DayOfWeek> getExistingScheduleDays() {
        return this.workSchedules.stream()
                .map(WorkSchedule::getDayOfWeek)
                .collect(Collectors.toSet());
    }
}