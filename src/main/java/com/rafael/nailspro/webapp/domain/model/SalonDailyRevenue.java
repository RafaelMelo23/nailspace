package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.infrastructure.dto.dashboard.DailyRevenueDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "salon_daily_revenue")
public class SalonDailyRevenue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDate date;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalRevenue;

    private Long appointmentsCount;

    public SalonDailyRevenue(String tenantId, LocalDate date) {
        setTenantId(tenantId);
        this.date = date;
        this.totalRevenue = BigDecimal.ZERO;
        this.appointmentsCount = 0L;
    }

    public static List<DailyRevenueDTO> mapToChartData(List<SalonDailyRevenue> dailyRevenues) {
        return dailyRevenues.stream()
                .map(drv -> DailyRevenueDTO.builder()
                        .date(drv.getDate())
                        .value(drv.getTotalRevenue())
                        .appointmentsCount(drv.getAppointmentsCount())
                        .build()
                )
                .toList();
    }

    public static BigDecimal calculateAvgTicket(BigDecimal total, Long count) {
        if (count == null || count == 0) return BigDecimal.ZERO;

        return total.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateRevenue(List<SalonDailyRevenue> dailyRevenues,
                                              LocalDate start,
                                              LocalDate end) {
        return dailyRevenues.stream()
                .filter(drv ->
                        !drv.getDate().isBefore(start) &&
                        !drv.getDate().isAfter(end)
                )
                .map(SalonDailyRevenue::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}