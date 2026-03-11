package com.rafael.nailspro.webapp.application.appointment.booking;

import com.rafael.nailspro.webapp.application.appointment.BookingPolicyService;
import com.rafael.nailspro.webapp.application.professional.ProfessionalWorkScheduleUseCase;
import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.application.salon.business.SalonServiceService;
import com.rafael.nailspro.webapp.domain.AvailabilityDomainService;
import com.rafael.nailspro.webapp.domain.model.*;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.domain.repository.ClientRepository;
import com.rafael.nailspro.webapp.domain.repository.ProfessionalRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AppointmentCreateDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.date.TimeInterval;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingAppointmentUseCase {

    private final ClientRepository clientRepository;
    private final AppointmentRepository repository;
    private final ProfessionalWorkScheduleUseCase professionalWorkScheduleUseCase;
    private final AvailabilityDomainService availabilityDomainService;
    private final SalonProfileService salonProfileService;
    private final BookingPolicyService bookingPolicyManager;
    private final ProfessionalRepository professionalRepository;
    private final SalonServiceService salonService;

    @Transactional
    public void bookAppointment(
            AppointmentCreateDTO dto,
            UserPrincipal principal
    ) {

        UUID professionalId = UUID.fromString(dto.professionalExternalId());

        Professional professional =
                professionalRepository.findByExternalIdWithPessimisticLock(professionalId);

        SalonService mainService =
                salonService.findById(dto.mainServiceId());

        List<AppointmentAddOn> addOns =
                salonService.findAddOns(dto.addOnsIds());

        Client client = clientRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado"));

        SalonProfile salonProfile =
                salonProfileService.getByTenantId(principal.getTenantId());

        TimeInterval interval =
                Appointment.calculateIntervalAndBuffer(
                        dto,
                        Appointment.calculateDurationInSeconds(mainService, addOns),
                        salonProfile);

        professionalWorkScheduleUseCase.checkProfessionalAvailability(professionalId, interval);

        availabilityDomainService.checkIfProfessionalHasTimeConflicts(professionalId, interval);

        bookingPolicyManager.validate(
                dto.zonedAppointmentDateTime().toLocalDateTime(),
                principal
        );

        Appointment appointment =
                Appointment.create(
                        dto,
                        client,
                        professional,
                        mainService,
                        addOns,
                        salonProfile,
                        interval
                );

        repository.save(appointment);
    }
}