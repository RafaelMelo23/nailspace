package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> getClientAppointmentsById(Long id, Pageable pageable);

    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.client.id = :userId
            """)
    Page<Appointment> findByClientId(@Param("userId") Long userId, Pageable pageable);

    List<Appointment> findByProfessional_IdAndStartDateBetween(Long professionalId,
                                                               Instant startDateAfter,
                                                               Instant startDateBefore);

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

    @Query("""
            SELECT ap FROM Appointment ap
            WHERE ap.startDate > :now
            AND ap.startDate <= :windowEnd
            AND NOT EXISTS(
            SELECT 1 FROM AppointmentNotification an
            WHERE an.appointment = ap
            AND an.notificationType = 'REMINDER'
            AND (an.notificationStatus = 'SENT' OR an.attempts >= 3)
                        )
            """)
    List<Appointment> findAppointmentsNeedingReminder(@Param("now") Instant now,
                                                      @Param("windowEnd") Instant windowEnd);

    Optional<Appointment> findFirstByClientIdOrderByStartDateDesc(Long clientId);

    double countByClientIdAndAppointmentStatus(Long clientId, AppointmentStatus attr0);


    @Query("SELECT a FROM Appointment a WHERE a.professional.id = :id " +
            "AND a.startDate < :endRange AND a.endDate > :startRange")
    List<Appointment> findBusyAppointmentsInRange(@Param("id") Long professionalId,
                                                  @Param("startRange") Instant startRange,
                                                  @Param("endRange") Instant endRange);

    @Query("SELECT ap FROM Appointment ap WHERE ap.id = :appointmentId AND ap.client.id = :clientId")
    Optional<Appointment> findAndValidateClientOwnership(@Param("appointmentId") Long appointmentId,
                                                         @Param("clientId") Long clientId);

    @Query("SELECT ap FROM Appointment ap WHERE ap.id = :appointmentId AND ap.professional.id = :professionalId")
    Optional<Appointment> findAndValidateProfessionalOwnership(@Param("appointmentId") Long appointmentId,
                                                               @Param("professionalId") Long professionalId);
}