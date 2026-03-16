package com.rafael.nailspro.webapp.application.professional;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.ScheduleBlock;
import com.rafael.nailspro.webapp.domain.repository.ProfessionalRepository;
import com.rafael.nailspro.webapp.domain.repository.ScheduleBlockRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockOutDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalScheduleBlockUseCase {

    private final ScheduleBlockRepository repository;
    private final ProfessionalRepository professionalRepository;
    private final SalonProfileService salonProfileService;

    public void createBlock(ScheduleBlockDTO blockDTO, Long professionalId) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado(a)"));

        repository.save(ScheduleBlock.createBlock(blockDTO, professional));
    }

    @Transactional
    public void deleteBlock(Long blockId, Long professionalId) {
        repository.deleteByIdAndProfessionalId(blockId, professionalId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleBlockOutDTO> getBlocks(Long professionalId, Optional<LocalDateTime> from) {
        ZoneId salonZoneId = salonProfileService.getSalonZoneIdByContext();

        Instant fromInstant = from
                .map(f -> f.atZone(salonZoneId).toInstant())
                .orElse(Instant.MIN);

        return repository.findByProfessional_IdAndDateStartTimeGreaterThanEqual(professionalId, fromInstant)
                .stream()
                .map(sb -> ScheduleBlockOutDTO.fromEntity(sb, salonZoneId))
                .collect(Collectors.toList());
    }
}