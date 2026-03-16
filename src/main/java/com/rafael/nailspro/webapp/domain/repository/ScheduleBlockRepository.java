package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.model.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    List<ScheduleBlock> findByProfessional_IdAndDateStartTimeGreaterThanEqual(Long professionalId, Instant from);

    @Query("SELECT sb FROM ScheduleBlock sb WHERE sb.professional.id = :prof " +
            "AND sb.dateStartTime < :end AND sb.dateEndTime > :start")
    List<ScheduleBlock> findBusyBlocksInRange(@Param("prof") Long professionalId,
                                              @Param("start") Instant startRange,
                                              @Param("end") Instant endRange);

    @Modifying
    @Query("DELETE FROM ScheduleBlock sb WHERE sb.id = :blockId AND sb.professional.id = :professionalId")
    void deleteByIdAndProfessionalId(@Param("blockId") Long blockId,
                                     @Param("professionalId") Long professionalId);
}
