package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.infrastructure.dto.salon.service.SalonServiceDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLRestriction;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
@Table(name = "service",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_service_name_per_tenant",
                        columnNames = {"tenant_Id", "name"}
                ),
                @UniqueConstraint(
                        name = "uk_service_desc_per_tenant",
                        columnNames = {"tenant_Id", "description"}
                )
        })
@Filter(name = "tenantFilter",
        condition = "tenant_id = :tenantId"
)
public class SalonService extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", nullable = false, length = 250)
    private String description;

    @Column(name = "nail_count", nullable = false)
    private Integer nailCount;

    @Column(name = "duration_in_seconds", nullable = false)
    private Integer durationInSeconds;

    @Column(name = "value", nullable = false)
    private Integer value;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "maintenance_interval_days")
    private Integer maintenanceIntervalDays;

    @Column(name = "requires_loyalty")
    private boolean  requiresLoyalty = false;

    @Column(name = "is_add_on")
    private boolean isAddOn;

    @ManyToMany
    @JoinTable(name = "service_professionals",
            joinColumns = @JoinColumn(name = "salonService_id"),
            inverseJoinColumns = @JoinColumn(name = "professionals_id"))
    private Set<Professional> professionals = new LinkedHashSet<>();

    @Override
    public void prePersist() {
        this.active = true;
        this.isDeleted = false;
    }

    public static SalonService create(SalonServiceDTO dto,
                                      Set<Professional> professionals) {

        return SalonService.builder()
                .name(dto.name())
                .description(dto.description())
                .value(dto.value())
                .durationInSeconds(dto.durationInSeconds())
                .maintenanceIntervalDays(dto.maintenanceIntervalDays())
                .requiresLoyalty(dto.requiresLoyalty() != null ? dto.requiresLoyalty() : false)
                .isAddOn(dto.isAddOn() != null ? dto.isAddOn() : false)
                .professionals(professionals)
                .nailCount(0)
                .build();
    }
}