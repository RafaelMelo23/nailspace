package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Professional p set p.isActive = false WHERE p.id = :id")
    void deactivateProfessional(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Professional p set p.isActive = true WHERE p.id = :id")
    void activateProfessional(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            SELECT (COUNT(p) > 0)
            FROM Professional p
            WHERE p.externalId = :externalId
            AND (
            EXISTS (
            SELECT 1 FROM Appointment ap
            WHERE ap.professional = p
            AND ap.appointmentStatus IN :statuses
            AND ap.endDate > :startDateAndTime
            AND ap.startDate < :endDateAndTime
            )
             OR (
            EXISTS (
            SELECT 1 From ScheduleBlock sb
            WHERE sb.professional = p
            AND sb.startTime < :endDateAndTime
            AND sb.endTime > :startDateAndTime
                )
            )
                        )
            """)
    boolean hasTimeConflicts(@Param("externalId") UUID professionalId,
                             @Param("startDateAndTime") Instant startDate,
                             @Param("endDateAndTime") Instant endDate,
                             @Param("statuses") List<AppointmentStatus> status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")})
    @Query("SELECT p FROM Professional p WHERE p.externalId = :id")
    Professional findByExternalIdWithPessimisticLock(@Param("id") UUID externalId);

    @IgnoreTenantFilter
    @Query("SELECT sp.owner from SalonProfile sp WHERE sp.tenantId = :id")
    Optional<Professional> findSalonOwnerByTenantId(@Param("id") String tenantId);

    Optional<Professional> findByExternalId(UUID uuid);

    Optional<Professional> findByEmailIgnoreCase(String email);


}
