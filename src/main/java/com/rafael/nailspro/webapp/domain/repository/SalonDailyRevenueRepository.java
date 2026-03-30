package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.model.SalonDailyRevenue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalonDailyRevenueRepository extends JpaRepository<SalonDailyRevenue, Long> {

    Optional<SalonDailyRevenue> findByTenantIdAndDate(String tenantId, LocalDate date);

    List<SalonDailyRevenue> findByTenantIdAndDateBetween(String tenantId, LocalDate startDate, LocalDate endDate);
}
