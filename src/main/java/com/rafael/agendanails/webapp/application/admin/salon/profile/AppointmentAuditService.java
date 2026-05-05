package com.rafael.agendanails.webapp.application.admin.salon.profile;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.domain.repository.AppointmentSpecification;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AddOnDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AdminUserAppointmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentAuditService {

    private final AppointmentRepository repository;
    private final SalonProfileService salonProfileService;

    @Transactional(readOnly = true)
    public Page<AdminUserAppointmentDTO> searchAppointments(Long professionalId, AppointmentStatus status, LocalDate date, Pageable pageable) {
        ZoneId salonZoneId = salonProfileService.getSalonZoneIdByContext();

        Specification<Appointment> spec = AppointmentSpecification.fetchSummaryRelationships()
                .and(AppointmentSpecification.withProfessionalId(professionalId))
                .and(AppointmentSpecification.withStatus(status))
                .and(AppointmentSpecification.withDate(date, salonZoneId));

        return repository.findAll(spec, pageable).map(ap -> mapToDTO(ap, salonZoneId));
    }

    @Transactional(readOnly = true)
    public Page<AdminUserAppointmentDTO> listUsersAppointments(Long userId, Pageable pageable) {

        ZoneId salonZoneId = salonProfileService.getSalonZoneIdByContext();

        return repository.findByClientId(userId, pageable)
                .map(ap -> mapToDTO(ap, salonZoneId));
    }

    private AdminUserAppointmentDTO mapToDTO(Appointment ap, ZoneId salonZoneId) {
        return AdminUserAppointmentDTO.builder()
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
                .build();
    }
}
