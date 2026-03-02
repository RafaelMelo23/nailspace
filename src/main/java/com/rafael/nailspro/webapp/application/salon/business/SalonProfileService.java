package com.rafael.nailspro.webapp.application.salon.business;

import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.nailspro.webapp.domain.model.BaseEntity;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.domain.repository.SalonProfileRepository;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SalonProfileService {

    private final SalonProfileRepository repository;

    public SalonProfile getByTenantId(String tenantId) {

        return repository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("Salão não encontrado"));
    }

    public boolean isAutoConfirmationEnabled(String tenantId) {

        return repository.isAutoConfirmationEnabledForTenant(tenantId);
    }

    public TenantStatus getStatusByTenantId(String tenantId) {

        return repository.findStatusByTenantId(tenantId);
    }

    public void save(SalonProfile salonProfile) {

        repository.save(salonProfile);
    }

    public String getTenantId(BaseEntity baseEntity) {

        return baseEntity.getTenantId();
    }

    public boolean isSalonOpenByTenantId(String tenantId) {

        return repository.existsSalonProfileByTenantIdAndOperationalStatus(tenantId, OperationalStatus.OPEN);
    }

    public String getSalonOperationalMessageByTenantId(String tenantId) {

        return repository.findWarningMessageByTenantId(tenantId)
                .orElse(null);
    }

    public Integer getSalonBufferTimeInMinutes(String tenantId) {

        return repository.findSalonProfileAppointmentBufferMinutesByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("Salão não encontrado."));
    }

    public ZoneId getSalonZoneIdByContext() {

        return repository.fetchZoneIdByTenantId(TenantContext.getTenant())
                .map(ZoneId::of)
                .orElseThrow(() -> new BusinessException("Fuso horário não encontrado."));
    }

    public ZoneId getSalonZoneId(String tenantId) {

        return repository.fetchZoneIdByTenantId(tenantId)
                .map(ZoneId::of)
                .orElseThrow(() -> new BusinessException("Fuso horário não encontrado."));
    }

    public SalonProfile findWithOwnerByTenantId(String tenantId) {

        return repository.findByTenantIdWithOwner(tenantId)
                .orElseThrow(() -> new BusinessException("Salão não encontrado"));
    }
}