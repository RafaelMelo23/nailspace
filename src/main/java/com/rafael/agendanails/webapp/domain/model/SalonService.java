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
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service")
@SQLDelete(sql = "UPDATE service SET deleted = true WHERE id = ?")
@Filter(name = "deletedFilter")
public class SalonService extends BaseEntity {

    @Builder.Default
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

    @Setter(AccessLevel.PRIVATE)
    @ManyToMany
    @JoinTable(name = "service_professionals",
            joinColumns = @JoinColumn(name = "salonService_id"),
            inverseJoinColumns = @JoinColumn(name = "professionals_id"))
    private Set<Professional> professionals = new LinkedHashSet<>();

    @Override
    public void prePersist() {
        super.prePersist();
        this.active = true;
    }

    public static SalonService create(SalonServiceDTO dto,
                                      Set<Professional> professionals) {

        return SalonService.builder()
                .name(dto.name())
                .description(dto.description())
                .value(dto.value())
                .durationInSeconds(dto.durationInSeconds())
                .maintenanceIntervalDays(dto.maintenanceIntervalDays())
                .isAddOn(dto.isAddOn() != null ? dto.isAddOn() : false)
                .professionals(professionals)
                .nailCount(0)
                .build();
    }

    public void setProfessionals(Set<Professional> newProfessionals) {
        if (newProfessionals.isEmpty()) return;

        this.getProfessionals().clear();
        this.professionals.addAll(newProfessionals);
    }

    public void validateCanBePerformedBy(Professional professional) {
        if (!this.getProfessionals().contains(professional)) {
            throw new BusinessException("O profissional selecionado não realiza este serviço: " + this.getName());
        }
    }
}