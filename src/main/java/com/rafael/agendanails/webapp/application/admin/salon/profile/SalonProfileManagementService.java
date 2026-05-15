package com.rafael.agendanails.webapp.application.admin.salon.profile;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.repository.SalonProfileRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.admin.salon.profile.SalonProfileDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalonProfileManagementService {

    private final SalonProfileRepository repository;
    private final SalonProfileService salonProfileService;

    @Transactional(readOnly = true)
    public SalonProfileDTO getProfile(String tenantId) {
        SalonProfile salonProfile = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("O perfil do salão não foi encontrado."));

        return SalonProfileDTO.builder()
                .tradeName(salonProfile.getTradeName())
                .slogan(salonProfile.getSlogan())
                .primaryColor(salonProfile.getPrimaryColor())
                .comercialPhone(salonProfile.getComercialPhone())
                .fullAddress(salonProfile.getFullAddress())
                .socialMediaLink(salonProfile.getSocialMediaLink())
                .status(salonProfile.getOperationalStatus())
                .warningMessage(salonProfile.getWarningMessage())
                .appointmentBufferMinutes(salonProfile.getAppointmentBufferMinutes())
                .zoneId(salonProfile.getZoneId())
                .isLoyalClientelePrioritized(salonProfile.isLoyalClientelePrioritized())
                .loyalClientBookingWindowDays(salonProfile.getLoyalClientBookingWindowDays())
                .standardBookingWindow(salonProfile.getStandardBookingWindow())
                .connectionState(salonProfile.getEvolutionConnectionState())
                .autoConfirmationAppointment(salonProfile.isAutoConfirmationAppointment())
                .build();
    }

    @Transactional
    public void updateProfile(String tenantId, SalonProfileDTO profileDTO) {
        SalonProfile salonProfile = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("O perfil do salão não foi encontrado."));

        salonProfile.updateBasicInfo(
                profileDTO.tradeName(),
                profileDTO.slogan(),
                profileDTO.comercialPhone(),
                profileDTO.fullAddress(),
                profileDTO.socialMediaLink()
        );

        salonProfile.updateAppearance(
                profileDTO.primaryColor(),
                null // logoPath update not implemented in DTO
        );

        salonProfile.updateOperationalStatus(
                profileDTO.status(),
                profileDTO.warningMessage()
        );

        salonProfile.configureScheduling(
                profileDTO.appointmentBufferMinutes(),
                profileDTO.zoneId(),
                profileDTO.autoConfirmationAppointment()
        );

        salonProfile.configureLoyaltyPriority(
                profileDTO.isLoyalClientelePrioritized(),
                profileDTO.loyalClientBookingWindowDays(),
                profileDTO.standardBookingWindow()
        );

        validateLoyalClientFeature(profileDTO);

        salonProfileService.save(salonProfile);
    }

    private static void validateLoyalClientFeature(SalonProfileDTO profile) {
        if (Boolean.TRUE.equals(profile.isLoyalClientelePrioritized()) && (
                profile.loyalClientBookingWindowDays() == null ||
                        profile.standardBookingWindow() == null)) {
            throw new BusinessException("""
                    O número de dias de antecedência
                    para clientes fiéis deve ser informado
                    quando a priorização de clientes fiéis estiver ativada.""");
        }
    }
}