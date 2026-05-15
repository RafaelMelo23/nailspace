package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
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

    @lombok.Builder(builderMethodName = "testBuilder")
    public ScheduleBlock(Long id, Instant startTime, Instant endTime, Boolean isWholeDayBlocked, String reason, Professional professional, String tenantId) {
        super(tenantId);
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isWholeDayBlocked = isWholeDayBlocked;
        this.reason = reason;
        this.professional = professional;
    }

    public void assignProfessional(Professional professional) {
        this.professional = professional;
    }

    public void updateDetails(Instant startTime, Instant endTime, Boolean isWholeDayBlocked, String reason) {
        if (isWholeDayBlocked != null && !isWholeDayBlocked && startTime == null) {
            throw new BusinessException("Data e hora de início são obrigatórias para bloqueios parciais.");
        }
        
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (isWholeDayBlocked != null) this.isWholeDayBlocked = isWholeDayBlocked;
        if (reason != null) this.reason = reason;
    }

    public static ScheduleBlock createBlock(ScheduleBlockDTO blockDTO, Professional professional) {

        if (!blockDTO.isWholeDayBlocked() && blockDTO.startTime() == null) {
            throw new BusinessException("Data e hora de início são obrigatórias para bloqueios parciais.");
        }

        ScheduleBlock block = new ScheduleBlock();
        block.reason = blockDTO.reason();
        block.professional = professional;
        block.isWholeDayBlocked = blockDTO.isWholeDayBlocked();
        block.startTime = blockDTO.startTime() != null ? blockDTO.startTime().toInstant() : null;
        block.endTime = blockDTO.endTime() != null ? blockDTO.endTime().toInstant() : null;
        block.assignTenant(professional.getTenantId());

        return block;
    }
}