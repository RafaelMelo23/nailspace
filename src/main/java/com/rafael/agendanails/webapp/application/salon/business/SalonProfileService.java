package com.rafael.agendanails.webapp.application.salon.business;

import com.rafael.agendanails.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.agendanails.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.agendanails.webapp.domain.model.BaseEntity;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.repository.SalonProfileRepository;
import com.rafael.agendanails.webapp.infrastructure.config.CacheConfig;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.exception.TenantNotFoundException;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SalonProfileService {

    private final SalonProfileRepository repository;
    private SalonProfileService self;

    @Autowired
    public void setSelf(@Lazy SalonProfileService self) {
        this.self = self;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetWhatsappConnectionState(String tenantId) {
        SalonProfile salon = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Salão não encontrado"));

        salon.setWhatsappLastResetAt(LocalDateTime.now());
        salon.setEvolutionConnectionState(EvolutionConnectionState.CLOSE);
        self.save(salon);
    }

    @Cacheable(value = CacheConfig.SALON_PROFILE_CACHE, key = "'profile:' + #tenantId")
    public SalonProfile getByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Salão não encontrado"));
    }

    @Cacheable(value = CacheConfig.SALON_PROFILE_CACHE, key = "'profile:' + #tenantId")
    public SalonProfile getByTenantIdElseNull(String tenantId) {
        return repository.findByTenantId(tenantId)
                .orElse(null);
    }

    @Cacheable(value = CacheConfig.SALON_PROFILE_CACHE, key = "'status:' + #tenantId")
    public TenantStatus getStatusByTenantId(String tenantId) {
        return repository.findStatusByTenantId(tenantId)
                .orElse(TenantStatus.ACTIVE);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'profile:' + #salonProfile.tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'status:' + #salonProfile.tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'isOpen:' + #salonProfile.tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'zone:' + #salonProfile.tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'withOwner:' + #salonProfile.tenantId")
    })
    public void save(SalonProfile salonProfile) {
        repository.save(salonProfile);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'profile:' + #tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'status:' + #tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'isOpen:' + #tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'zone:' + #tenantId"),
            @CacheEvict(value = CacheConfig.SALON_PROFILE_CACHE, key = "'withOwner:' + #tenantId")
    })
    public void evictCache(String tenantId) {
    }

    public String getTenantId(BaseEntity baseEntity) {
        return baseEntity.getTenantId();
    }

    @Cacheable(value = CacheConfig.SALON_PROFILE_CACHE, key = "'isOpen:' + #tenantId")
    public boolean isSalonOpenByTenantId(String tenantId) {
        return repository.existsSalonProfileByTenantIdAndOperationalStatus(tenantId, OperationalStatus.OPEN);
    }

    public ZoneId getSalonZoneIdByContext() {
        return repository.fetchZoneIdByTenantId(TenantContext.getTenant())
                .map(ZoneId::of)
                .orElseThrow(() -> new BusinessException("Fuso horário não encontrado."));
    }

    @Cacheable(value = CacheConfig.SALON_PROFILE_CACHE, key = "'zone:' + #tenantId")
    public ZoneId getSalonZoneId(String tenantId) {
        return repository.fetchZoneIdByTenantId(tenantId)
                .map(ZoneId::of)
                .orElseThrow(() -> new BusinessException("Fuso horário não encontrado."));
    }

    @Cacheable(value = CacheConfig.SALON_PROFILE_CACHE, key = "'withOwner:' + #tenantId")
    public SalonProfile findWithOwnerByTenantId(String tenantId) {
        return repository.findByTenantIdWithOwner(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Salão não encontrado"));
    }
}