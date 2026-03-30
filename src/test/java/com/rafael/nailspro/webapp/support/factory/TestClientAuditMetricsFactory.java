package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.domain.model.ClientAuditMetrics;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class TestClientAuditMetricsFactory {

    public static ClientAuditMetrics create(String tenantId, Client client) {
        ClientAuditMetrics cam = new ClientAuditMetrics(tenantId, client);
        cam.setTotalSpent(new BigDecimal("100.00"));
        cam.setCompletedAppointmentsCount(5L);
        cam.setCanceledAppointmentsCount(1L);
        cam.setMissedAppointmentsCount(0L);
        cam.setLastVisitDate(ZonedDateTime.now());
        return cam;
    }
}
