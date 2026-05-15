package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.infrastructure.dto.salon.service.SalonServiceDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "service")
@SQLDelete(sql = "UPDATE service SET deleted = true WHERE id = ?")
@Filter(name = "deletedFilter")
public class SalonService extends BaseEntity {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", nullable = false, length = 250)
    private String description;

    @Column(name = "nail_count")
    private Integer nailCount;

    @Column(name = "duration_in_seconds", nullable = false)
    private Integer durationInSeconds;

    @Column(name = "value", nullable = false)
    private Integer value;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "maintenance_interval_days")
    private Integer maintenanceIntervalDays;

    @Column(name = "is_add_on")
    private boolean isAddOn;

    @ManyToMany
    @JoinTable(name = "service_professionals",
            joinColumns = @JoinColumn(name = "salonService_id"),
            inverseJoinColumns = @JoinColumn(name = "professionals_id"))
    private Set<Professional> professionals = new LinkedHashSet<>();

    @Builder(builderMethodName = "testBuilder")
    public SalonService(Long id, String name, String description, Integer nailCount, Integer durationInSeconds, Integer value, Boolean active, Integer maintenanceIntervalDays, boolean isAddOn, Set<Professional> professionals, String tenantId) {
        super(tenantId);
        this.id = id;
        this.name = name;
        this.description = description;
        this.nailCount = nailCount;
        this.durationInSeconds = durationInSeconds;
        this.value = value;
        this.active = active;
        this.maintenanceIntervalDays = maintenanceIntervalDays;
        this.isAddOn = isAddOn;
        this.professionals = professionals != null ? professionals : new LinkedHashSet<>();
    }

    @Override
    public void prePersist() {
        super.prePersist();
        if (this.active == null) this.active = true;
    }

    public static SalonService create(SalonServiceDTO dto,
                                      Set<Professional> professionals) {

        SalonService service = new SalonService();
        service.name = dto.name();
        service.description = dto.description();
        service.value = dto.value();
        service.durationInSeconds = dto.durationInSeconds();
        service.maintenanceIntervalDays = dto.maintenanceIntervalDays();
        service.isAddOn = dto.isAddOn() != null ? dto.isAddOn() : false;
        service.professionals = professionals;
        service.nailCount = 0;
        service.active = true;
        return service;
    }

    public void updateInfo(String name, String description, Integer maintenanceIntervalDays) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (maintenanceIntervalDays != null) this.maintenanceIntervalDays = maintenanceIntervalDays;
    }

    public void updatePricingAndDuration(Integer value, Integer durationInSeconds) {
        if (value != null) this.value = value;
        if (durationInSeconds != null) this.durationInSeconds = durationInSeconds;
    }

    public void updateStatus(boolean active) {
        this.active = active;
    }

    public void toggleAddOn(boolean isAddOn) {
        this.isAddOn = isAddOn;
    }

    public void assignProfessionals(Set<Professional> newProfessionals) {
        if (newProfessionals == null) return;

        this.getProfessionals().clear();
        this.professionals.addAll(newProfessionals);
    }

    public void validateCanBePerformedBy(Professional professional) {
        if (!this.getProfessionals().contains(professional)) {
            throw new BusinessException("O profissional selecionado não realiza este serviço: " + this.getName());
        }
    }
}