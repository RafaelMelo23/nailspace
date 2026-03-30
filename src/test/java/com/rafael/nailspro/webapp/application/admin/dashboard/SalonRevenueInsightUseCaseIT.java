package com.rafael.nailspro.webapp.application.admin.dashboard;

import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestSalonDailyRevenueFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalonRevenueInsightUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private SalonRevenueInsightUseCase useCase;

    @Test
    void shouldReturnMonthlyRevenueDataCorrectly() {
        String tenantId = "tenant-test";
        LocalDate now = LocalDate.now();

        var day1 = TestSalonDailyRevenueFactory.create(tenantId, now, new BigDecimal("100.00"), 2L);
        var day2 = TestSalonDailyRevenueFactory.create(tenantId, now.minusDays(5), new BigDecimal("150.00"), 3L);
        var day3 = TestSalonDailyRevenueFactory.create(tenantId, now.minusDays(10), new BigDecimal("200.00"), 4L);

        salonDailyRevenueRepository.saveAll(List.of(day1, day2, day3));

        var dashboard = useCase.getMonthlySalonRevenue(tenantId);

        assertThat(dashboard.monthlyRevenue()).isEqualByComparingTo("450.00");
        assertThat(dashboard.weeklyRevenue()).isEqualByComparingTo("250.00");
        assertThat(dashboard.monthlyAppointmentsCount()).isEqualTo(9L);
        assertThat(dashboard.averageTicket()).isEqualByComparingTo("50.00");
        assertThat(dashboard.chartData()).hasSize(3);
    }

    @Test
    void shouldIsolateDataBetweenTenants() {
        String tenantA = "tenant-a";
        String tenantB = "tenant-b";
        LocalDate now = LocalDate.now();

        var dayA = TestSalonDailyRevenueFactory.create(tenantA, now, new BigDecimal("100.00"), 1L);
        var dayB = TestSalonDailyRevenueFactory.create(tenantB, now, new BigDecimal("500.00"), 1L);

        salonDailyRevenueRepository.saveAll(List.of(dayA, dayB));

        TenantContext.setTenant(tenantA);
        var dashboardA = useCase.getMonthlySalonRevenue(tenantA);

        assertThat(dashboardA.monthlyRevenue()).isEqualByComparingTo("100.00");
        assertThat(dashboardA.monthlyAppointmentsCount()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenNoDataIsFound() {
        assertThatThrownBy(() -> useCase.getMonthlySalonRevenue("non-existent"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não foi possível encontrar métricas para este salão.");
    }
}
