package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.enums.appointment.RetentionStatus;
import com.rafael.nailspro.webapp.domain.model.RetentionForecast;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RetentionForecastRepository extends JpaRepository<RetentionForecast, Long> {

    @IgnoreTenantFilter
    @Query("""
            SELECT rf FROM RetentionForecast rf
            WHERE rf.predictedReturnDate >= :start
            AND rf.predictedReturnDate < :end
            AND rf.status = :status
            """)
    List<RetentionForecast> findAllPredictedForecastsBetween(@Param("start") Instant start,
                                                             @Param("end") Instant end,
                                                             @Param("status") RetentionStatus status);

    @IgnoreTenantFilter
    @Query("SELECT rf FROM RetentionForecast rf WHERE rf.predictedReturnDate < :now AND rf.status IN :statuses")
    List<RetentionForecast> findAllExpiredPredictedForecastsByStatus(@Param("now") Instant now,
                                                                     @Param("statuses") List<RetentionStatus> statuses);

    @IgnoreTenantFilter
    int deleteByPredictedReturnDateBefore(Instant predictedReturnDateBefore);

    @IgnoreTenantFilter
    @Query("""
            SELECT rf
            FROM RetentionForecast rf
            LEFT JOIN FETCH rf.client
            LEFT JOIN FETCH rf.originAppointment
            LEFT JOIN FETCH rf.salonServices
            WHERE rf.id = :id
            """)
    Optional<RetentionForecast> findWithJoins(@Param("id") Long id);

    @IgnoreTenantFilter
    @Modifying
    @Query("UPDATE RetentionForecast rt SET rt.status = :status WHERE rt.id = :id")
    void updateStatus(@Param("id") Long retentionId,
                             @Param("status") RetentionStatus status);
}