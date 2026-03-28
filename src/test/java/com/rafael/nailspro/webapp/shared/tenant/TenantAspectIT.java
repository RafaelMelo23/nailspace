package com.rafael.nailspro.webapp.shared.tenant;

import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("it")
class TenantAspectIT extends BaseIntegrationTest {

    @Autowired
    private IgnoreFilterTestService testService;

    @Test
    void invokeWithoutTenant_shouldThrowException_whenNotIgnored() {
        TenantContext.clear();
        
        assertThatThrownBy(() -> clientRepository.findAll())
                .isInstanceOf(com.rafael.nailspro.webapp.infrastructure.exception.TenantNotFoundException.class);
    }

    @Test
    void invokeWithTenantFilter_shouldFilterResults_whenTenantIsSet() {
        TenantContext.setTenant("TENANT_A");
        Client clientA = TestClientFactory.standardForIt();
        clientA.setTenantId("TENANT_A");
        clientRepository.save(clientA);

        TenantContext.setTenant("TENANT_B");
        Client clientB = TestClientFactory.standardForIt();
        clientB.setTenantId("TENANT_B");
        clientRepository.save(clientB);

        TenantContext.setTenant("TENANT_A");

        List<Client> foundClients = clientRepository.findAll();

        assertThat(foundClients).hasSize(1);
        assertThat(foundClients.get(0).getTenantId()).isEqualTo("TENANT_A");
    }

    @Test
    void invokeWithTenantFilter_shouldNotFilterResults_whenIgnoreAnnotationIsPresent() {
        TenantContext.setTenant("TENANT_A");
        Client clientA = TestClientFactory.standardForIt();
        clientA.setTenantId("TENANT_A");
        clientRepository.save(clientA);

        TenantContext.setTenant("TENANT_B");
        Client clientB = TestClientFactory.standardForIt();
        clientB.setTenantId("TENANT_B");
        clientRepository.save(clientB);

        TenantContext.clear();

        List<Client> foundClients = testService.findAllIgnored();

        assertThat(foundClients).hasSize(2);
    }

    @Test
    void invokeWithTenantFilter_shouldIgnoreFilter_whenMethodIsFindByEmailIgnoreCase() {
        TenantContext.setTenant("TENANT_A");
        Client clientA = TestClientFactory.standardForIt();
        clientA.setTenantId("TENANT_A");
        clientA.setEmail("user@tenantA.com");
        clientRepository.save(clientA);

        TenantContext.setTenant("TENANT_B");
        Client clientB = TestClientFactory.standardForIt();
        clientB.setTenantId("TENANT_B");
        clientB.setEmail("user@tenantB.com");
        clientRepository.save(clientB);

        TenantContext.setTenant("TENANT_A");
        TenantContext.setIgnoreFilter(true);

        Optional<Client> foundClientB = clientRepository.findByEmailIgnoreCase("user@tenantB.com");

        assertThat(foundClientB).isPresent();
        assertThat(foundClientB.get().getTenantId()).isEqualTo("TENANT_B");
    }
}
