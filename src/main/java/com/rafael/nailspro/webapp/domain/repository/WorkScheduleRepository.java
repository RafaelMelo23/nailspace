package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.model.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    List<WorkSchedule> findByProfessional_Id(Long professionalId);

    @Query("""
            SELECT COUNT(ws) > 0
            FROM WorkSchedule ws
            WHERE ws.professional.externalId = :externalId
            AND ws.dayOfWeek = :day
            AND ws.workStart < :startTime
            AND ws.workEnd > :endTime
            AND (
                :endTime < ws.lunchBreakStartTime OR
                :startTime > ws.lunchBreakEndTime
                )""")
    boolean checkIfProfessionalIsAvailable(@Param("externalId") UUID professionalId,
                                           @Param("startTime") LocalTime startTime,
                                           @Param("endTime") LocalTime endTime,
                                           @Param("day") DayOfWeek day);

    @Modifying
    @Query("""
            DELETE FROM WorkSchedule ws
            WHERE ws.id = :scheduleId
            AND ws.professional.id = :professionalId
            """)
    void deleteByIdAndProfessional(Long scheduleId, Long professionalId);
}
