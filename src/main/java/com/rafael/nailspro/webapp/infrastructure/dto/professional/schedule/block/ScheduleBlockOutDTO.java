package com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block;

import com.rafael.nailspro.webapp.domain.model.ScheduleBlock;
import lombok.Builder;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Builder
public record ScheduleBlockOutDTO(Long id,
                                  ZonedDateTime dateAndStartTime,
                                  ZonedDateTime dateAndEndTime,
                                  Boolean isWholeDayBlocked,
                                  String reason,
                                  Long professionalId) {

    public static ScheduleBlockOutDTO fromEntity(ScheduleBlock sb, ZoneId salonZoneId) {
        return ScheduleBlockOutDTO.builder()
                .id(sb.getId())
                .dateAndStartTime(ZonedDateTime.ofInstant(sb.getDateStartTime(), salonZoneId))
                .dateAndEndTime(ZonedDateTime.ofInstant(sb.getDateEndTime(), salonZoneId))
                .isWholeDayBlocked(sb.getIsWholeDayBlocked())
                .professionalId(sb.getProfessional().getId())
                .reason(sb.getReason())
                .build();
    }
}
