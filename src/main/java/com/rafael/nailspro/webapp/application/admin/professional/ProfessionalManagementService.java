package com.rafael.nailspro.webapp.application.admin.professional;

import com.rafael.nailspro.webapp.application.salon.business.SalonServiceService;
import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonService;
import com.rafael.nailspro.webapp.domain.repository.ProfessionalRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.admin.professional.CreateProfessionalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfessionalManagementService {

    private final ProfessionalRepository professionalRepository;
    private final SalonServiceService salonServiceService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createProfessional(CreateProfessionalDTO professionalDTO, String tenantId) {

        Set<SalonService> services =
                getServicesOfferedByProfessional(professionalDTO);

        professionalRepository.save(Professional.builder()
                .salonServices(services)
                .fullName(professionalDTO.fullName())
                .email(professionalDTO.email())
                .password(passwordEncoder.encode("mudar123"))
                .userRole(UserRole.PROFESSIONAL)
                .status(UserStatus.ACTIVE)
                .tenantId(tenantId)
                .build());
    }

    private Set<SalonService> getServicesOfferedByProfessional(CreateProfessionalDTO professionalDTO) {
        return new HashSet<>(salonServiceService.findByIdIn(
                professionalDTO.servicesOfferedByProfessional()
                        .stream()
                        .toList()
        ));
    }

    @Transactional
    public void deactivateProfessional(Long id) {
        professionalRepository.deactivateProfessional(id);
    }
}