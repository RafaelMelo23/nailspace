package com.rafael.nailspro.webapp.application.admin.dashboard;

import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestClientAuditMetricsFactory;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientInsightServiceIT extends BaseIntegrationTest {

    @Autowired
    private ClientInsightService clientInsightService;

    @Test
    void shouldReturnClientAuditedInfoCorrectly() {
        var client = clientRepository.save(TestClientFactory.standardForIt());
        var metrics = clientAuditMetricsRepository.save(TestClientAuditMetricsFactory.create("tenant-test", client));

        var result = clientInsightService.getClientsAuditedInfo(client.getId());

        assertThat(result.clientId()).isEqualTo(client.getId());
        assertThat(result.name()).isEqualTo(client.getFullName());
        assertThat(result.totalSpent()).isEqualByComparingTo("100.00");
        assertThat(result.completedAppointments()).isEqualTo(5L);
    }

    @Test
    void shouldIsolateDataBetweenTenants() {
        // Tenant A
        var clientA = clientRepository.save(TestClientFactory.standardForIt());
        clientAuditMetricsRepository.save(TestClientAuditMetricsFactory.create("tenant-test", clientA));

        // Tenant B
        TenantContext.setTenant("tenant-b");
        var clientB = clientRepository.save(TestClientFactory.standardForIt());
        clientAuditMetricsRepository.save(TestClientAuditMetricsFactory.create("tenant-b", clientB));

        // Attempt to access Client B's info from Tenant A
        TenantContext.setTenant("tenant-test");
        assertThatThrownBy(() -> clientInsightService.getClientsAuditedInfo(clientB.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldThrowExceptionWhenClientMetricsNotFound() {
        assertThatThrownBy(() -> clientInsightService.getClientsAuditedInfo(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O cliente não foi encontrado, ou não tem métricas registradas.");
    }
}
