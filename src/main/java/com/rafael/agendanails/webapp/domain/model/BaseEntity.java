package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;


@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@Filter(name = "tenantFilter")
public class BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    public void assignTenant(String tenantId) {
        this.tenantId = tenantId;
    }

    protected void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenant();
        }
    }
}