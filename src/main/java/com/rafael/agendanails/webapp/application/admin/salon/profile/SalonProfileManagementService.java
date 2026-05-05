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

import java.util.function.Consumer;

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

        setIfNotNull(profileDTO.tradeName(), salonProfile::setTradeName);
        setIfNotNull(profileDTO.slogan(), salonProfile::setSlogan);
        setIfNotNull(profileDTO.primaryColor(), salonProfile::setPrimaryColor);
        setIfNotNull(profileDTO.comercialPhone(), salonProfile::setComercialPhone);
        setIfNotNull(profileDTO.fullAddress(), salonProfile::setFullAddress);
        setIfNotNull(profileDTO.socialMediaLink(), salonProfile::setSocialMediaLink);
        setIfNotNull(profileDTO.warningMessage(), salonProfile::setWarningMessage);
        setIfNotNull(profileDTO.appointmentBufferMinutes(), salonProfile::setAppointmentBufferMinutes);
        setIfNotNull(profileDTO.zoneId(), salonProfile::setZoneId);
        setIfNotNull(profileDTO.isLoyalClientelePrioritized(), salonProfile::setLoyalClientelePrioritized);
        setIfNotNull(profileDTO.loyalClientBookingWindowDays(), salonProfile::setLoyalClientBookingWindowDays);
        setIfNotNull(profileDTO.standardBookingWindow(), salonProfile::setStandardBookingWindow);
        setIfNotNull(profileDTO.autoConfirmationAppointment(), salonProfile::setAutoConfirmationAppointment);
        setIfNotNull(profileDTO.status(), salonProfile::setOperationalStatus);

        removeWarningMessageIfSalonIsOpen(profileDTO, salonProfile);

        validateLoyalClientFeature(profileDTO);

        salonProfileService.save(salonProfile);
    }

    private static void removeWarningMessageIfSalonIsOpen(SalonProfileDTO profileDTO, SalonProfile salonProfile) {
        if (profileDTO.status() != OperationalStatus.OPEN) {
            return;
        }

        if (salonProfile.getWarningMessage() != null) {
            salonProfile.setWarningMessage(null);
        }
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

    public <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }
}