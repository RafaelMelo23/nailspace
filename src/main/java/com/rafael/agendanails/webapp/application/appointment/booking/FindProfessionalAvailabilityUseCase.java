package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.AvailabilityDomainService;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.domain.repository.SalonServiceRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentTimesDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.ProfessionalAvailabilityDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.FindProfessionalAvailabilityDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Professional professional = professionalRepository.findByExternalId(UUID.fromString(dto.professionalExternalId()))
                .orElseThrow(() -> new BusinessException("Professional not found"));

        String tenantId = professional.getTenantId();
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        log.debug("Searching availability: Professional={}, Client={}", dto.professionalExternalId(), userId);

        SalonProfile salonProfile = salonProfileService.getByTenantId(tenantId);

        List<SalonService> services = salonServiceRepository.findAllById(dto.servicesIds());

        AppointmentTimeWindow appointmentTimeWindow = bookingPolicyManager.calculateAllowedWindow(services, tenantId, userId);

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(appointmentTimeWindow)
                .salonProfile(salonProfile)
                .serviceDurationInSeconds(dto.serviceDurationInSeconds())
                .build();

        List<AppointmentTimesDTO> availableTimes = availabilityDomainService.findAvailableTimes(query);

        log.debug("Found {} available slots for Professional={}", availableTimes.size(), dto.professionalExternalId());

        return ProfessionalAvailabilityDTO.builder()
                .appointmentTimesDTOList(availableTimes)
                .zoneId(salonProfile.getZoneId())
                .earliestRecommendedDate(getEarliestRecommendedDate(userId, salonProfile).orElse(null))
                .build();
    }

    private Optional<ZonedDateTime> getEarliestRecommendedDate(Long userId, SalonProfile salonProfile) {
        if (userId == null) return Optional.empty();

        Instant recommendedDate = bookingPolicyManager.calculateEarliestRecommendedDate(userId);

        if (recommendedDate != null) {
            return Optional.of(recommendedDate.atZone(salonProfile.getZoneId()));
        }
        return Optional.empty();
    }
}