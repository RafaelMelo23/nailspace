package com.rafael.agendanails.webapp.application.admin.client;

import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.domain.repository.ClientRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.admin.client.ClientAppointmentDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.admin.client.ClientDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class ClientManagementService {

    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public void updateClientStatus(Long clientId, UserStatus status) {

        clientRepository.updateClientStatus(clientId, status);
    }

    public Page<ClientDTO> findClientsForManagement(String clientName, Pageable pageable) {

        return clientRepository
                .findByFullNameContainingIgnoreCase(clientName, pageable)
                .map(cl -> ClientDTO.builder()
                        .clientId(cl.getId())
                        .fullName(cl.getFullName())
                        .email(cl.getEmail())
                        .phoneNumber(cl.getPhoneNumber())
                        .missedAppointments(cl.getMissedAppointments())
                        .userStatus(cl.getStatus())
                        .build());
    }

    public Page<ClientAppointmentDTO> getClientAppointmentHistory(Long clientId, Pageable pageable) {
        return appointmentRepository
                .getClientAppointmentsById(clientId, pageable)
                .map(ap -> ClientAppointmentDTO.builder()
                        .appointmentId(ap.getId())

                        .professionalId(ap.getProfessional().getId())
                        .professionalName(ap.getProfessional().getFullName())

                        .startDateAndTime(ZonedDateTime.ofInstant(ap.getStartDate(), ap.getSalonZoneId()))
                        .status(ap.getAppointmentStatus())

                        .mainServiceName(ap.getMainSalonService().getName())
                        .addOnServiceNames(
                                ap.getAddOns()
                                        .stream()
                                        .map(app -> app.getService().getName())
                                        .toList()
                        )

                        .totalValue(ap.getTotalValue())
                        .observations(ap.getObservations())
                        .build()
                );
    }
}