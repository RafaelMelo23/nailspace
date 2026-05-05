package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    @Query(value = """
            SELECT ap
            FROM Appointment ap
            LEFT JOIN FETCH ap.professional
            LEFT JOIN FETCH ap.mainSalonService
            WHERE ap.client.id = :id
            """,
            countQuery = "SELECT COUNT(ap) FROM Appointment ap WHERE ap.client.id = :id")
    Page<Appointment> getClientAppointmentsById(@Param("id") Long id, Pageable pageable);

    @Query(value = """
            SELECT a
            FROM Appointment a
            LEFT JOIN FETCH a.mainSalonService
            LEFT JOIN FETCH a.professional
            LEFT JOIN FETCH a.client
            WHERE a.client.id = :userId
            """,
            countQuery = "SELECT COUNT(a) FROM Appointment a WHERE a.client.id = :userId")
    Page<Appointment> findByClientId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT a FROM Appointment a
            LEFT JOIN FETCH a.mainSalonService
            LEFT JOIN FETCH a.addOns ao
            LEFT JOIN FETCH ao.service
            LEFT JOIN FETCH a.client
            LEFT JOIN FETCH a.professional
            WHERE a.id = :id
            """)
    Optional<Appointment> findFullById(Long id);

    @IgnoreTenantFilter
    @Query("""
            SELECT ap FROM Appointment ap
            WHERE ap.appointmentStatus = com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus.CONFIRMED
            AND ap.startDate > :now
            AND ap.startDate <= :windowEnd
            AND NOT EXISTS (
                SELECT 1 FROM WhatsappMessage wm
                WHERE wm.appointment = ap
                AND wm.messageType = com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageType.REMINDER
            )
            """)
    List<Appointment> findAppointmentsNeedingReminder(@Param("now") Instant now,
                                                      @Param("windowEnd") Instant windowEnd);

    Optional<Appointment> findFirstByClientIdAndAppointmentStatusOrderByStartDateDesc(Long clientId, AppointmentStatus status);

    double countByClientIdAndAppointmentStatus(Long clientId, AppointmentStatus attr0);


    @Query("""
    SELECT a FROM Appointment a WHERE a.professional.id = :id
    AND a.startDate < :endRange
    AND a.endDate > :startRange
    AND a.appointmentStatus IN :statuses""")
    List<Appointment> findBusyAppointmentsInRange(@Param("id") Long professionalId,
                                                  @Param("startRange") Instant startRange,
                                                  @Param("endRange") Instant endRange,
                                                  @Param("statuses") List<AppointmentStatus> status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ap FROM Appointment ap WHERE ap.id = :appointmentId AND ap.client.id = :clientId")
    Optional<Appointment> findAndValidateClientOwnership(@Param("appointmentId") Long appointmentId,
                                                         @Param("clientId") Long clientId);

    @Query("SELECT ap FROM Appointment ap WHERE ap.id = :appointmentId AND ap.professional.id = :professionalId")
    Optional<Appointment> findAndValidateProfessionalOwnership(@Param("appointmentId") Long appointmentId,
                                                               @Param("professionalId") Long professionalId);
}
