package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.agendanails.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalonProfileRepository extends JpaRepository<SalonProfile, Long> {

    @Query("SELECT sp FROM SalonProfile sp WHERE sp.owner.id = :id")
    Optional<SalonProfile> findByOwner_Id(@Param("id") Long id);

    @IgnoreTenantFilter
    @Query("SELECT s.appointmentBufferMinutes FROM SalonProfile s WHERE s.tenantId = :tenantId")
    Optional<Integer> findSalonProfileAppointmentBufferMinutesByTenantId(@Param("tenantId") String tenantId);

    @IgnoreTenantFilter
    boolean existsSalonProfileByTenantIdAndOperationalStatus(String tenantId, OperationalStatus operationalStatus);

    @IgnoreTenantFilter
    @Query("SELECT s.warningMessage FROM SalonProfile s WHERE s.tenantId = :tenantId")
    Optional<String> findWarningMessageByTenantId(@Param("tenantId") String tenantId);

    @IgnoreTenantFilter
    @Query("SELECT s.zoneId FROM SalonProfile s WHERE s.tenantId = :tenantId")
    Optional<String> fetchZoneIdByTenantId(@Param("tenantId") String tenantId);

    @IgnoreTenantFilter
    Optional<SalonProfile> findByTenantId(String tenantId);

    @IgnoreTenantFilter
    @Query("SELECT sp FROM SalonProfile sp JOIN FETCH sp.owner WHERE sp.tenantId = :tenantId")
    Optional<SalonProfile> findByTenantIdWithOwner(String tenantId);

    @IgnoreTenantFilter
    @Query("SELECT sp.tenantStatus FROM SalonProfile sp WHERE sp.tenantId = :id")
    TenantStatus findStatusByTenantId(@Param("id") String tenantId);

    @IgnoreTenantFilter
    @Query("SELECT sp.autoConfirmationAppointment FROM SalonProfile sp WHERE sp.tenantId = :tenantId")
    boolean isAutoConfirmationEnabledForTenant(@Param("tenantId") String tenantId);

    @IgnoreTenantFilter
    @Query("SELECT sp.tradeName FROM SalonProfile sp WHERE sp.tenantId = :id")
    Optional<String> findSalonTradeName(@Param("id") String tenantId);

    @Query("SELECT sp.primaryColor FROM SalonProfile sp WHERE sp.tenantId = :id")
    Optional<String> findPrimaryColorByTenantId(@Param("id") String tenantId);
}