package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedule_block")
public class ScheduleBlock extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "is_whole_day_blocked")
    private Boolean isWholeDayBlocked = Boolean.FALSE;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    public static ScheduleBlock createBlock(ScheduleBlockDTO blockDTO, Professional professional) {

        ScheduleBlock block = ScheduleBlock.builder()
                .reason(blockDTO.reason())
                .professional(professional)
                .isWholeDayBlocked(blockDTO.isWholeDayBlocked())
                .startTime(blockDTO.startTime().toInstant())
                .endTime(blockDTO.endTime().toInstant())
                .build();

        if (!block.getIsWholeDayBlocked() && block.getStartTime() == null) {
            throw new BusinessException("Data e hora de início são obrigatórias para bloqueios totais.");
        }

        return block;
    }



}