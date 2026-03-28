package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;


@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@SuperBuilder
@Getter @Setter
@MappedSuperclass
@Filter(name = "tenantFilter")
public class BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenant();
        }
    }
}