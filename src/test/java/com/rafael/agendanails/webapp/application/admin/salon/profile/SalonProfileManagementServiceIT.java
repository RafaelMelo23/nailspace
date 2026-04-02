package com.rafael.agendanails.webapp.application.admin.salon.profile;

import com.rafael.agendanails.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.infrastructure.dto.admin.salon.profile.SalonProfileDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.files.FileUploadService;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestProfessionalFactory;
import com.rafael.agendanails.webapp.support.factory.TestSalonProfileFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SalonProfileManagementServiceIT extends BaseIntegrationTest {

    @Autowired
    private SalonProfileManagementService salonProfileManagementService;

    @MockitoBean
    private FileUploadService fileUploadService;

    @Test
    void shouldUpdateProfile() throws IOException {
        Professional owner = professionalRepository.save(TestProfessionalFactory.builder().build());
        SalonProfile profile = salonProfileRepository.save(TestSalonProfileFactory.standardForIT(owner));

        SalonProfileDTO dto = SalonProfileDTO.builder()
                .tradeName("Updated Name")
                .slogan("Updated Slogan")
                .status(OperationalStatus.CLOSED_TEMPORARY)
                .appointmentBufferMinutes(30)
                .build();

        salonProfileManagementService.updateProfile(owner.getId(), dto);

        SalonProfile updatedProfile = salonProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updatedProfile.getTradeName()).isEqualTo("Updated Name");
        assertThat(updatedProfile.getSlogan()).isEqualTo("Updated Slogan");
        assertThat(updatedProfile.getOperationalStatus()).isEqualTo(OperationalStatus.CLOSED_TEMPORARY);
        assertThat(updatedProfile.getAppointmentBufferMinutes()).isEqualTo(30);
    }

    @Test
    void shouldThrowExceptionWhenLoyaltyPrioritizedButWindowsAreMissing() {
        Professional owner = professionalRepository.save(TestProfessionalFactory.builder().build());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(owner));

        SalonProfileDTO dto = SalonProfileDTO.builder()
                .isLoyalClientelePrioritized(true)
                .loyalClientBookingWindowDays(null)
                .standardBookingWindow(null)
                .build();

        assertThatThrownBy(() -> salonProfileManagementService.updateProfile(owner.getId(), dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O número de dias de antecedência");
    }

    @Test
    void shouldNotUpdateProfileFromAnotherTenant() throws IOException {
        String tenantA = "tenant-a";
        String tenantB = "tenant-b";

        TenantContext.setTenant(tenantA);
        Professional ownerA = professionalRepository.save(TestProfessionalFactory.builder().tenantId(tenantA).build());
        SalonProfile profileA = salonProfileRepository.save(TestSalonProfileFactory.standardForIT(ownerA, tenantA));

        SalonProfileDTO dto = SalonProfileDTO.builder()
                .tradeName("Hacked Name")
                .build();

        TenantContext.setTenant(tenantB);
        
        assertThatThrownBy(() -> salonProfileManagementService.updateProfile(ownerA.getId(), dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O perfil do salão não foi encontrado.");

        TenantContext.setTenant(tenantA);
        SalonProfile updatedProfile = salonProfileRepository.findById(profileA.getId()).orElseThrow();
        assertThat(updatedProfile.getTradeName()).isNotEqualTo("Hacked Name");
    }
}
