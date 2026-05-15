package com.rafael.agendanails.webapp.shared.tenant;

import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.infrastructure.exception.TenantNotFoundException;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
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
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void invokeWithTenantFilter_shouldFilterResults_whenTenantIsSet() {
        TenantContext.setTenant("TENANT_A");
        Client clientA = TestClientFactory.standardForIt();
        clientA.assignTenant("TENANT_A");
        clientRepository.save(clientA);

        TenantContext.setTenant("TENANT_B");
        Client clientB = TestClientFactory.standardForIt();
        clientB.assignTenant("TENANT_B");
        clientRepository.save(clientB);

        TenantContext.setTenant("TENANT_A");

        List<Client> foundClients = clientRepository.findAll();

        assertThat(foundClients).hasSize(1);
        assertThat(foundClients.get(0).getTenantId()).isEqualTo("TENANT_A");
    }

    @Test
    void invokeWithTenantFilter_shouldNotFilterResults_whenIgnoreAnnotationIsPresentInService() {
        TenantContext.setTenant("TENANT_A");
        Client clientA = TestClientFactory.standardForIt();
        clientA.assignTenant("TENANT_A");
        clientRepository.save(clientA);

        TenantContext.setTenant("TENANT_B");
        Client clientB = TestClientFactory.standardForIt();
        clientB.assignTenant("TENANT_B");
        clientRepository.save(clientB);

        TenantContext.clear();
        List<Client> foundClients = testService.findAllIgnored();
        assertThat(foundClients).hasSize(2);

        TenantContext.setTenant("TENANT_A");
        Optional<Client> foundClientB = testService.findByEmailIgnored(clientB.getEmail());
        assertThat(foundClientB).isPresent();
        assertThat(foundClientB.get().getTenantId()).isEqualTo("TENANT_B");
    }

    @Test
    void invokeWithTenantFilter_shouldFilterResults_whenIgnoreAnnotationIsNOTPresentInServiceOrRepo() {
        TenantContext.setTenant("TENANT_A");
        Client clientA = TestClientFactory.standardForIt();
        clientA.assignTenant("TENANT_A");
        clientRepository.save(clientA);

        TenantContext.setTenant("TENANT_B");
        Client clientB = TestClientFactory.standardForIt();
        clientB.assignTenant("TENANT_B");
        clientRepository.save(clientB);

        TenantContext.setTenant("TENANT_A");

        Optional<Client> foundClientB = testService.findByEmailNotIgnored(clientB.getEmail());
        assertThat(foundClientB).isEmpty();

        Optional<Client> foundClientA = testService.findByEmailNotIgnored(clientA.getEmail());
        assertThat(foundClientA).isPresent();
    }

    @Test
    void invokeWithTenantFilter_shouldIgnoreFilter_whenMethodIsFindByEmailIgnoreCaseInUserRepository() {
        TenantContext.setTenant("TENANT_A");
        Client clientA = TestClientFactory.standardForIt();
        clientA.assignTenant("TENANT_A");
        clientRepository.save(clientA);

        TenantContext.setTenant("TENANT_B");
        Client clientB = TestClientFactory.standardForIt();
        clientB.assignTenant("TENANT_B");
        clientRepository.save(clientB);

        TenantContext.setTenant("TENANT_A");

        Optional<User> foundClientB = userRepository.findByEmailIgnoreCase(clientB.getEmail());

        assertThat(foundClientB).isPresent();
        assertThat(foundClientB.get().getTenantId()).isEqualTo("TENANT_B");
    }
}
