package com.rafael.agendanails.webapp.infrastructure.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

@Builder
public record AppointmentCreateDTO(
        @NotBlank(message = "O identificador do profissional é obrigatório")
        String professionalExternalId,

        @NotNull(message = "O serviço principal é obrigatório")
        Long mainServiceId,

        List<Long> addOnsIds,

        @NotNull(message = "A data e horário do agendamento são obrigatórios")
        ZonedDateTime zonedAppointmentDateTime,

        String observation
) {}