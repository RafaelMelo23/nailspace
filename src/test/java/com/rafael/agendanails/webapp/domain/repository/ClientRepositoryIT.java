package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRepositoryIT extends BaseIntegrationTest {

    @Test
    void shouldNotFindClientFromAnotherTenantByPhone() {
        String phoneNumber = "5511999999999";
        TenantContext.setTenant("tenant-a");
        Client clientA = TestClientFactory.standardForIt("tenant-a");
        clientA.updatePhoneNumber(phoneNumber);
        clientRepository.save(clientA);

        TenantContext.setTenant("tenant-b");
        Optional<Client> found = clientRepository.findByPhoneNumber(phoneNumber);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldUpdateClientStatus() {
        TenantContext.setTenant("tenant-test");
        Client client = clientRepository.save(TestClientFactory.standardForIt("tenant-test"));
        assertThat(client.getStatus()).isEqualTo(UserStatus.ACTIVE);

        clientRepository.updateClientStatus(client.getId(), UserStatus.BANNED);

        Client updated = clientRepository.findById(client.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserStatus.BANNED);
    }

    @Test
    void shouldIncrementCanceledAppointments() {
        TenantContext.setTenant("tenant-test");
        Client client = clientRepository.save(TestClientFactory.standardForIt("tenant-test"));
        assertThat(client.getCanceledAppointments()).isZero();

        clientRepository.incrementCanceledAppointments(client.getId());
        entityManager.flush();
        entityManager.clear();

        Client updated = clientRepository.findById(client.getId()).orElseThrow();
        assertThat(updated.getCanceledAppointments()).isEqualTo(1);
    }

    @Test
    void shouldFindByFullNameWithTenantIsolation() {
        String name = "Maria";
        
        TenantContext.setTenant("tenant-a");
        Client clientA = TestClientFactory.standardForIt("tenant-a");
        clientA.updateFullName("Maria Silva");
        clientRepository.save(clientA);

        TenantContext.setTenant("tenant-b");
        Client clientB = TestClientFactory.standardForIt("tenant-b");
        clientB.updateFullName("Maria Oliveira");
        clientRepository.save(clientB);

        TenantContext.setTenant("tenant-a");
        Page<Client> resultsA = clientRepository.findByFullNameContainingIgnoreCase(name, PageRequest.of(0, 10));

        assertThat(resultsA.getContent()).hasSize(1);
        assertThat(resultsA.getContent().get(0).getFullName()).isEqualTo("Maria Silva");
    }
}
