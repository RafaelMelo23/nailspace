package com.rafael.nailspro.webapp.application.internal;

import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.infrastructure.dto.onboarding.OnboardingRequestDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.onboarding.OnboardingResultDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnboardingServiceIT extends BaseIntegrationTest {

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldOnboardSuccessfullyWhenDataIsValid() {
        OnboardingRequestDTO dto = OnboardingRequestDTO.builder()
                .fullName("Salon Owner")
                .email("owner@salon.com")
                .domainSlug("my-salon")
                .build();

        OnboardingResultDTO result = onboardingService.onboardOwner(dto);

        assertThat(result.owner().getFullName()).isEqualTo("Salon Owner");
        assertThat(result.owner().getEmail()).isEqualTo("owner@salon.com");
        assertThat(result.owner().getUserRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.owner().getTenantId()).isEqualTo("my-salon");
        assertThat(passwordEncoder.matches("mudar123", result.owner().getPassword())).isTrue();

        assertThat(result.profile().getTenantId()).isEqualTo("my-salon");
        assertThat(result.profile().getOwner().getId()).isEqualTo(result.owner().getId());

        Optional<Professional> persistedOwner = professionalRepository.findById(result.owner().getId());
        assertThat(persistedOwner).isPresent();
        assertThat(persistedOwner.get().getTenantId()).isEqualTo("my-salon");

        Optional<SalonProfile> persistedProfile = salonProfileRepository.findById(result.profile().getId());
        assertThat(persistedProfile).isPresent();
    }

    @Test
    void shouldSanitizeDomainSlugWhenSlugContainsSpecialChars() {
        OnboardingRequestDTO dto = OnboardingRequestDTO.builder()
                .fullName("Salon Owner")
                .email("owner@salon.com")
                .domainSlug("My Salon @ 2026!!!")
                .build();

        OnboardingResultDTO result = onboardingService.onboardOwner(dto);

        assertThat(result.owner().getTenantId()).isEqualTo("my-salon-2026");
        assertThat(result.profile().getTenantId()).isEqualTo("my-salon-2026");
    }

    @Test
    void shouldThrowExceptionWhenDomainSlugIsInvalidAfterSanitization() {
        OnboardingRequestDTO dto = OnboardingRequestDTO.builder()
                .fullName("Salon Owner")
                .email("owner@salon.com")
                .domainSlug("!!! @@@")
                .build();

        assertThatThrownBy(() -> onboardingService.onboardOwner(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Slug do domínio não pode estar vazia");
    }
}
