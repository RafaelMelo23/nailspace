package com.rafael.agendanails.webapp.application.internal;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.domain.repository.SalonProfileRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.onboarding.OnboardingRequestDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.onboarding.OnboardingResultDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@IgnoreTenantFilter
public class OnboardingService {

    private final ProfessionalRepository professionalRepository;
    private final SalonProfileRepository salonProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public OnboardingResultDTO onboardOwner(OnboardingRequestDTO dto) {
        String tenantId = buildSanitizedDomainSlug(dto);

        Professional owner = createSalonOwner(dto, tenantId);
        SalonProfile profile = createSalonProfile(owner, tenantId);

        return assembleOnboardingResult(owner, profile);
    }

    private Professional createSalonOwner(OnboardingRequestDTO dto, String tenantId) {
        Professional owner = Professional.builder()
                .fullName(dto.fullName())
                .email(dto.email())
                .password(passwordEncoder.encode("mudar123"))
                .userRole(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .tenantId(tenantId)
                .build();

        return professionalRepository.save(owner);
    }

    private SalonProfile createSalonProfile(Professional owner, String tenantId) {
        SalonProfile profile = SalonProfile.builder()
                .owner(owner)
                .tenantId(tenantId)
                .appointmentBufferMinutes(0)
                .build();

        return salonProfileRepository.save(profile);
    }

    private OnboardingResultDTO assembleOnboardingResult(Professional owner, SalonProfile profile) {
        return OnboardingResultDTO.builder()
                .profile(profile)
                .owner(owner)
                .build();
    }

    private static String buildSanitizedDomainSlug(OnboardingRequestDTO dto) {
        String sanitizedDomainSlug = dto.domainSlug()
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (sanitizedDomainSlug.isEmpty() || sanitizedDomainSlug.equals("-")) {
            throw new BusinessException("Slug do domínio não pode estar vazia");
        }
        return sanitizedDomainSlug;
    }
}