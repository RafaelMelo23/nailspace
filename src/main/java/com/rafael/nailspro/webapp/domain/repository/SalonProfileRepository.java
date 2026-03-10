package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalonProfileRepository extends JpaRepository<SalonProfile, Long> {

    Optional<SalonProfile> findByOwner_Id(Long id);

    @Query("SELECT s.appointmentBufferMinutes FROM SalonProfile s WHERE s.tenantId = :tenantId")
    Optional<Integer> findSalonProfileAppointmentBufferMinutesByTenantId(@Param("tenantId") String tenantId);

    boolean existsSalonProfileByTenantIdAndOperationalStatus(String tenantId, OperationalStatus operationalStatus);

    @Query("SELECT s.warningMessage FROM SalonProfile s WHERE s.tenantId = :tenantId")
    Optional<String> findWarningMessageByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT s.zoneId FROM SalonProfile s WHERE s.tenantId = :tenantId")
    Optional<String> fetchZoneIdByTenantId(@Param("tenantId") String tenantId);

    Optional<SalonProfile> findByTenantId(String tenantId);

    @Query("SELECT sp FROM SalonProfile sp JOIN FETCH sp.owner WHERE sp.tenantId = :tenantId")
    Optional<SalonProfile> findByTenantIdWithOwner(String tenantId);

    @Query("SELECT sp.tenantStatus FROM SalonProfile sp WHERE sp.tenantId = :id")
    TenantStatus findStatusByTenantId(@Param("id") String tenantId);

    @Query("SELECT sp.autoConfirmationAppointment FROM SalonProfile sp WHERE sp.tenantId = :tenantId")
    boolean isAutoConfirmationEnabledForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT sp.tradeName FROM SalonProfile sp WHERE sp.tenantId = :id")
    Optional<String> findSalonTradeName(@Param("id") String tenantId);
}