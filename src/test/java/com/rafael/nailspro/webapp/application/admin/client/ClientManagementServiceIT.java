package com.rafael.nailspro.webapp.application.admin.client;

import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.infrastructure.dto.admin.client.ClientDTO;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientManagementServiceIT extends BaseIntegrationTest {

    @Autowired
    private ClientManagementService clientManagementService;

    @Test
    void shouldFindClientsForManagementFilteringByNameAndTenant() {
        String tenantA = "tenant-a";
        String tenantB = "tenant-b";

        TenantContext.setTenant(tenantA);
        clientRepository.save(TestClientFactory.builder().fullName("Alice In Wonderland").tenantId(tenantA).email("alice@test.com").phoneNumber("1111111111111").build());
        clientRepository.save(TestClientFactory.builder().fullName("Bob The Builder").tenantId(tenantA).email("bob@test.com").phoneNumber("2222222222222").build());

        TenantContext.setTenant(tenantB);
        clientRepository.save(TestClientFactory.builder().fullName("Alice In Chains").tenantId(tenantB).email("alice2@test.com").phoneNumber("3333333333333").build());

        TenantContext.setTenant(tenantA);
        Page<ClientDTO> result = clientManagementService.findClientsForManagement("Alice", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).fullName()).isEqualTo("Alice In Wonderland");
    }

    @Test
    void shouldUpdateClientStatus() {
        Client client = clientRepository.save(TestClientFactory.builder().status(UserStatus.ACTIVE).build());

        clientManagementService.updateClientStatus(client.getId(), UserStatus.BANNED);

        Client updatedClient = clientRepository.findById(client.getId()).orElseThrow();
        assertThat(updatedClient.getStatus()).isEqualTo(UserStatus.BANNED);
    }

    @Test
    void shouldNotUpdateClientStatusFromAnotherTenant() {
        String tenantA = "tenant-a";
        String tenantB = "tenant-b";

        TenantContext.setTenant(tenantA);
        Client clientA = clientRepository.save(TestClientFactory.builder().tenantId(tenantA).build());

        TenantContext.setTenant(tenantB);
        clientManagementService.updateClientStatus(clientA.getId(), UserStatus.BANNED);

        TenantContext.setIgnoreFilter(true);
        Client updatedClientDirect = clientRepository.findById(clientA.getId()).orElseThrow();
        TenantContext.setIgnoreFilter(false);

        TenantContext.setTenant(tenantA);
        Client updatedClient = clientRepository.findById(clientA.getId()).orElseThrow();
        assertThat(updatedClient.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
