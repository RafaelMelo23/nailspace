package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.SalonDailyRevenue;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TestSalonDailyRevenueFactory {

    public static SalonDailyRevenue create(String tenantId, LocalDate date, BigDecimal totalRevenue, Long count) {
        SalonDailyRevenue drv = new SalonDailyRevenue(tenantId, date);
        drv.setTotalRevenue(totalRevenue);
        drv.setAppointmentsCount(count);
        return drv;
    }
}
