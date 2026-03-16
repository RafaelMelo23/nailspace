package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedule_block")
@Filter(name = "tenantFilter",
        condition = "tenant_id = :tenantId"
)
public class ScheduleBlock extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "date_and_start_time", nullable = false)
    private Instant dateStartTime;

    @Column(name = "date_and_end_time", nullable = false)
    private Instant dateEndTime;

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
                .dateStartTime(blockDTO.dateAndStartTime().toInstant())
                .dateEndTime(blockDTO.dateAndEndTime().toInstant())
                .build();

        if (!block.getIsWholeDayBlocked() && block.getDateStartTime() == null) {
            throw new BusinessException("Data e hora de início são obrigatórias para bloqueios totais.");
        }

        return block;
    }



}