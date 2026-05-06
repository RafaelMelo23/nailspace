package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.application.salon.business.SalonServiceService;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.domain.repository.ClientRepository;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentCreateDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingContextLoader {

    private final ClientRepository clientRepository;
    private final ProfessionalRepository professionalRepository;
    private final SalonServiceService salonService;
    private final SalonProfileService salonProfileService;

    public BookingContext load(AppointmentCreateDTO dto, UserPrincipal principal) {
        UUID professionalId = UUID.fromString(dto.professionalExternalId());

        Professional professional =
                professionalRepository.findByExternalIdWithPessimisticLock(professionalId);

        SalonService mainService =
                salonService.findById(dto.mainServiceId());
        mainService.validateCanBePerformedBy(professional);

        Set<SalonService> addOnServices =
                salonService.findAddOnsByIds(dto.addOnsIds());

        List<AppointmentAddOn> addOns = addOnServices.stream()
                .map(s -> AppointmentAddOn.create(s, professional))
                .toList();

        Client client = clientRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado"));
        client.validateCanBook();

        SalonProfile salonProfile =
                salonProfileService.getByTenantId(principal.getTenantId());

        TimeInterval interval =
                Appointment.calculateIntervalAndBuffer(
                        dto,
                        Appointment.calculateDurationInSeconds(mainService, addOns),
                        salonProfile);

        return new BookingContext(client, professional, mainService, addOns, salonProfile, interval);
    }
}
