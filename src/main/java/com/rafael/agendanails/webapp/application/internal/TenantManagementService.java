package com.rafael.agendanails.webapp.application.internal;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.agendanails.webapp.domain.repository.SalonProfileRepository;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@IgnoreTenantFilter
public class TenantManagementService {

    private final SalonProfileRepository salonProfileRepository;
    private final SalonProfileService salonProfileService;

    @Transactional
    public boolean updateTenantStatus(String tenantId, TenantStatus status) {
        return salonProfileRepository.findByTenantId(tenantId)
                .map(salon -> {
                    salon.setTenantStatus(status);
                    salonProfileService.save(salon);
                    return true;
                })
                .orElseThrow(() -> new BusinessException("Tenant não encontrado."));
    }
}