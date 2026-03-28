package com.rafael.nailspro.webapp.application.appointment.booking;

import com.rafael.nailspro.webapp.application.appointment.BookingPolicyService;
import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.domain.AvailabilityDomainService;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.domain.model.SalonService;
import com.rafael.nailspro.webapp.domain.model.UserPrincipal;
import com.rafael.nailspro.webapp.domain.repository.ProfessionalRepository;
import com.rafael.nailspro.webapp.domain.repository.SalonServiceRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AppointmentTimesDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.ProfessionalAvailabilityDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.FindProfessionalAvailabilityDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class FindProfessionalAvailabilityUseCase {

    private final AvailabilityDomainService availabilityDomainService;
    private final SalonServiceRepository salonServiceRepository;
    private final SalonProfileService salonProfileService;
    private final ProfessionalRepository professionalRepository;
    private final BookingPolicyService bookingPolicyManager;

    @Transactional(readOnly = true)
    public ProfessionalAvailabilityDTO findAvailableTimes(FindProfessionalAvailabilityDTO dto, UserPrincipal userPrincipal) {
        log.debug("Searching availability: Professional={}, Client={}", dto.professionalExternalId(), userPrincipal.getUserId());

        SalonProfile salonProfile = salonProfileService.getByTenantId(userPrincipal.getTenantId());

        Professional professional = professionalRepository.findByExternalId(UUID.fromString(dto.professionalExternalId()))
                .orElseThrow(() -> new BusinessException("Professional not found"));

        List<SalonService> services = salonServiceRepository.findAllById(dto.servicesIds());

        AppointmentTimeWindow appointmentTimeWindow = bookingPolicyManager.calculateAllowedWindow(services, userPrincipal);

        List<AppointmentTimesDTO> availableTimes = availabilityDomainService.findAvailableTimes(
                professional, appointmentTimeWindow, salonProfile, dto.serviceDurationInSeconds());

        log.debug("Found {} available slots for Professional={}", availableTimes.size(), dto.professionalExternalId());

        return ProfessionalAvailabilityDTO.builder()
                .appointmentTimesDTOList(availableTimes)
                .zoneId(salonProfile.getZoneId())
                .earliestRecommendedDate(getEarliestRecommendedDate(userPrincipal, salonProfile).orElse(null))
                .build();
    }

    private Optional<ZonedDateTime> getEarliestRecommendedDate(UserPrincipal userPrincipal, SalonProfile salonProfile) {
        Instant recommendedDate = bookingPolicyManager.calculateEarliestRecommendedDate(userPrincipal.getUserId());

        if (recommendedDate != null) {
            return Optional.of(recommendedDate.atZone(salonProfile.getZoneId()));
        }
        return Optional.empty();
    }
}