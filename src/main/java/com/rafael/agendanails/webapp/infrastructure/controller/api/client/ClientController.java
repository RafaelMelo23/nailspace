package com.rafael.agendanails.webapp.infrastructure.controller.api.client;

import com.rafael.agendanails.webapp.application.client.ClientAppointmentAuditingUseCase;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/client")
public class ClientController {

    private final ClientAppointmentAuditingUseCase clientAppointmentAuditingUseCase;

    @GetMapping("/appointments")
    public ResponseEntity<Page<AppointmentDTO>> getAppointments(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                @PageableDefault(
                                                                        sort = "startDate",
                                                                        direction = Sort.Direction.DESC)
                                                                Pageable pageable) {
        return ResponseEntity.ok(
                clientAppointmentAuditingUseCase.getClientsAppointments(userPrincipal.getId(), pageable)
        );
    }
}