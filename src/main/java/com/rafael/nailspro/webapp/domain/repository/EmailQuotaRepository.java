package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.model.EmailUsageQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface EmailQuotaRepository extends JpaRepository<EmailUsageQuota, Long> {

    boolean existsByUsageDate(LocalDate usageDate);

    Optional<Integer> getDailyUsageCount(LocalDate usageDate);

    @Query("SELECT COALESCE(SUM(e.dailyCount), 0) FROM EmailUsageQuota e WHERE e.usageDate >= :start AND e.usageDate <= :end")
    Optional<Integer> getMonthlyUsageCount(@Param("start")LocalDate start,
                             @Param("end")LocalDate end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailUsageQuota> findByUsageDate(LocalDate usageDate);
}