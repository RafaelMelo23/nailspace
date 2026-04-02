package com.rafael.agendanails.webapp.application.salon.business;

import com.rafael.agendanails.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.agendanails.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.agendanails.webapp.domain.model.BaseEntity;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.repository.SalonProfileRepository;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
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

    public SalonProfile getByTenantIdElseNull(String tenantId) {
        return repository.findByTenantId(tenantId)
                .orElse(null);
    }

    public String getCustomColor(String tenantId) {
        return repository.findPrimaryColorByTenantId(tenantId)
                .orElse(null);
    }

    public String getTradeNameByTenantId(String tenantId) {

        return repository.findSalonTradeName(tenantId)
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