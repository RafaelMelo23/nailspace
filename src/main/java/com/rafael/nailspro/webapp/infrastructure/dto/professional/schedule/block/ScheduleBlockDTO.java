package com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block;

import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record ScheduleBlockDTO(

        Long id,

        @NotNull(message = "A data e hora de início do bloqueio são obrigatórias.")
        ZonedDateTime dateAndStartTime,

        @NotNull(message = "A data e hora de término do bloqueio são obrigatórias.")
        ZonedDateTime dateAndEndTime,

        @NotNull(message = "É necessário informar se o bloqueio é para o dia inteiro.")
        Boolean isWholeDayBlocked,

        String reason
) {}
