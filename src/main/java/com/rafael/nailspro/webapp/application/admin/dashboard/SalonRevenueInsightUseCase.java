package com.rafael.nailspro.webapp.application.admin.dashboard;

import com.rafael.nailspro.webapp.domain.model.SalonDailyRevenue;
import com.rafael.nailspro.webapp.domain.repository.SalonDailyRevenueRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.dashboard.SalonDashboardDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.rafael.nailspro.webapp.domain.model.SalonDailyRevenue.*;

@Service
@RequiredArgsConstructor
public class SalonRevenueInsightUseCase {

    private final SalonDailyRevenueRepository repository;

    public SalonDashboardDTO getMonthlySalonRevenue(String tenantId) {
        var now = LocalDate.now();
        var thirtyDaysAgo = now.minusDays(30);
        var sevenDaysAgo = now.minusDays(7);

        List<SalonDailyRevenue> dailyRevenues = repository.findByTenantIdAndDateBetween(tenantId, thirtyDaysAgo, now);

        if (dailyRevenues == null || dailyRevenues.isEmpty()) {
            throw new BusinessException("Não foi possível encontrar métricas para este salão.");
        }

        BigDecimal monthlyRevenue = calculateRevenue(dailyRevenues, thirtyDaysAgo, now);
        BigDecimal weeklyRevenue = calculateRevenue(dailyRevenues, sevenDaysAgo, now);
        Long appointmentsCount = dailyRevenues.stream()
                .mapToLong(SalonDailyRevenue::getAppointmentsCount)
                .sum();
        BigDecimal averageTicket = calculateAvgTicket(monthlyRevenue, appointmentsCount);

        return SalonDashboardDTO.builder()
                .monthlyRevenue(monthlyRevenue)
                .weeklyRevenue(weeklyRevenue)
                .averageTicket(averageTicket)
                .monthlyAppointmentsCount(appointmentsCount)
                .chartData(mapToChartData(dailyRevenues))
                .build();
    }
}