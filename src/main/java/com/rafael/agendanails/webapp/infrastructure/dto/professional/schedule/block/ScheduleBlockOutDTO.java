package com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block;

import com.rafael.agendanails.webapp.domain.model.ScheduleBlock;
import lombok.Builder;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Builder
public record ScheduleBlockOutDTO(Long id,
                                  ZonedDateTime startTime,
                                  ZonedDateTime endTime,
                                  Boolean isWholeDayBlocked,
                                  String reason,
                                  Long professionalId) {

    public static ScheduleBlockOutDTO fromEntity(ScheduleBlock sb, ZoneId salonZoneId) {
        return ScheduleBlockOutDTO.builder()
                .id(sb.getId())
                .startTime(ZonedDateTime.ofInstant(sb.getStartTime(), salonZoneId))
                .endTime(ZonedDateTime.ofInstant(sb.getEndTime(), salonZoneId))
                .isWholeDayBlocked(sb.getIsWholeDayBlocked())
                .professionalId(sb.getProfessional().getId())
                .reason(sb.getReason())
                .build();
    }
}
