package com.rafael.nailspro.webapp.application.admin.salon.profile;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AddOnDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AdminUserAppointmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentAuditService {

    private final AppointmentRepository repository;
    private final SalonProfileService salonProfileService;

    @Transactional(readOnly = true)
    public Page<AdminUserAppointmentDTO> listUsersAppointments(Long userId, Pageable pageable) {

        ZoneId salonZoneId = salonProfileService.getSalonZoneIdByContext();

        return repository.findByClientId(userId, pageable)
                .map(ap -> AdminUserAppointmentDTO.builder()
                        .appointmentId(ap.getId())
                        .clientId(ap.getClient().getId())
                        .clientName(ap.getClient().getFullName())
                        .clientPhoneNumber(ap.getClient().getPhoneNumber())
                        .clientMissedAppointments(ap.getClient().getMissedAppointments())
                        .clientCanceledAppointments(ap.getClient().getCanceledAppointments())
                        .professionalId(ap.getProfessional().getId())
                        .professionalName(ap.getProfessional().getFullName())
                        .mainServiceId(ap.getMainSalonService().getId())
                        .mainServiceName(ap.getMainSalonService().getName())
                        .mainServiceDurationInSeconds(ap.getMainSalonService().getDurationInSeconds())
                        .mainServiceValue(ap.getMainSalonService().getValue())
                        .addOns(
                                ap.getAddOns().stream()
                                        .map(addOn -> new AddOnDTO(
                                                addOn.getService().getId(),
                                                addOn.getService().getName(),
                                                addOn.getQuantity(),
                                                addOn.getUnitPriceSnapshot()
                                        ))
                                        .toList()
                        )
                        .status(ap.getAppointmentStatus())
                        .totalValue(ap.calculateTotalValue())
                        .observations(ap.getObservations())
                        .startDateAndTime(ZonedDateTime.ofInstant(ap.getStartDate(), salonZoneId))
                        .endDateAndTime(ZonedDateTime.ofInstant(ap.getEndDate(), salonZoneId))
                        .build()
                );
    }
}