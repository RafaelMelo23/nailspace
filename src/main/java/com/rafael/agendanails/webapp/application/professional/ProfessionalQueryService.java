package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.ProfessionalResponseDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.ProfessionalSimplifiedDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.files.FileUploadService;
import com.rafael.agendanails.webapp.infrastructure.mapper.ProfessionalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalQueryService {

    private final ProfessionalRepository repository;
    private final FileUploadService fileUploadService;

    public List<ProfessionalSimplifiedDTO> findAllSimplified() {

        return repository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> ProfessionalSimplifiedDTO.builder()
                        .externalId(p.getExternalId())
                        .name(p.getFullName())
                        .professionalPicture(fileUploadService.getFileUrl(p.getProfessionalPicture()))
                        .build())
                .toList();
    }

    public Professional findByTenantId(String tenantId) {

        return repository.findSalonOwnerByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("Unable to find Owner"));
    }

    public List<ProfessionalResponseDTO> findAllProfessionals() {
        return ProfessionalMapper.toDTOList(repository.findAll());
    }
}