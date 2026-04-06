package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.TimeInterval;
import com.rafael.agendanails.webapp.domain.model.WorkSchedule;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.domain.repository.WorkScheduleRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.WorkScheduleRecordDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalWorkScheduleUseCase {

    private final WorkScheduleRepository repository;
    private final ProfessionalRepository professionalRepository;

    @Transactional
    public List<WorkSchedule> createSchedules(List<WorkScheduleRecordDTO> workScheduleDTO, Long professionalId) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado(a)"));

        try {
            return repository.saveAll(new HashSet<>(professional.registerNewSchedules(workScheduleDTO)));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Erro: Você já possui horários cadastrados para um ou mais dos dias selecionados.");
        }
    }

    @Transactional
    public void modifyWeekSchedule(List<WorkScheduleRecordDTO> dtos, Long professionalId) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado(a)"));

        Map<Long, WorkSchedule> existingById = professional.getWorkSchedules().stream()
                .collect(Collectors.toMap(WorkSchedule::getId, ws -> ws));

        Map<DayOfWeek, WorkSchedule> existingByDay = professional.getWorkSchedules().stream()
                .collect(Collectors.toMap(WorkSchedule::getDayOfWeek, ws -> ws));

        List<WorkScheduleRecordDTO> toCreate = new ArrayList<>();

        for (WorkScheduleRecordDTO dto : dtos) {
            if (dto.id() != null) {
                WorkSchedule existing = existingById.get(dto.id());
                if (existing == null || !existing.getProfessional().getId().equals(professionalId)) {
                    throw new BusinessException("Horário não encontrado ou acesso não permitido.");
                }
                existing.updateFromDto(dto);
            } else if (existingByDay.containsKey(dto.dayOfWeek())) {
                existingByDay.get(dto.dayOfWeek()).updateFromDto(dto);
            } else if (Boolean.TRUE.equals(dto.isActive())) {
                toCreate.add(dto);
            }
        }

        if (!toCreate.isEmpty()) {
            professional.registerNewSchedules(toCreate);
        }
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long professionalId) {
        repository.deleteByIdAndProfessional(scheduleId, professionalId);
    }

    public Set<WorkScheduleRecordDTO> getWorkSchedules(Long userId) {
        List<WorkSchedule> schedules = repository.findByProfessional_Id(userId);

        if (schedules.isEmpty()) {
            throw new BusinessException("Nenhum cronograma de trabalho encontrado para este profissional.");
        }

        return schedules.stream()
                .map(wsc ->
                        new WorkScheduleRecordDTO(
                                wsc.getId(),
                                wsc.getDayOfWeek(),
                                wsc.getWorkStart(),
                                wsc.getWorkEnd(),
                                wsc.getLunchBreakStartTime(),
                                wsc.getLunchBreakEndTime(),
                                wsc.getIsActive()))
                .collect(Collectors.toSet());
    }

    public void checkProfessionalAvailability(UUID professionalExternalId, TimeInterval interval) {
        if (!repository.checkIfProfessionalIsAvailable(professionalExternalId,
                interval.getStartTimeOnly(),
                interval.getEndTimeOnly(),
                interval.getDayOfWeek())) {
            throw new BusinessException("O profissional não está disponível neste período.");
        }
    }
}