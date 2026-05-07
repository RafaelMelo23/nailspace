package com.rafael.agendanails.webapp.application.salon.business;

import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.SalonService;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.domain.repository.SalonServiceRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.salon.service.SalonServiceDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.salon.service.SalonServiceOutDTO;
import com.rafael.agendanails.webapp.infrastructure.mapper.SalonServiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SalonServiceService {

    private final SalonServiceRepository salonServiceRepository;
    private final ProfessionalRepository professionalRepository;
    private final SalonServiceMapper salonServiceMapper;

    public SalonService save(SalonService service) {
        return salonServiceRepository.save(service);
    }

    public Set<SalonService> saveAll(Set<SalonService> services) {
        return new HashSet<>(salonServiceRepository.saveAll(new ArrayList<>(services)));
    }

    public SalonService findById(Long id) {

        return salonServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SalonService not found"));
    }

    public Set<SalonService> findAll() {
        return salonServiceRepository.findAllServices();
    }

    public Set<SalonService> findAddOnsByIds(List<Long> addOnIds) {
        if (addOnIds == null || addOnIds.isEmpty()) return new HashSet<>();
        return salonServiceRepository.findByIdIn(addOnIds);
    }

    public Set<SalonService> findByIdIn(List<Long> ids) {
        return salonServiceRepository.findByIdIn(ids);
    }

    @Transactional
    public void createService(SalonServiceDTO salonServiceDTO) {

        Set<Professional> professionalsThatOfferSuchService = getProfessionals(salonServiceDTO);

        SalonService builtService = SalonService.create(salonServiceDTO, professionalsThatOfferSuchService);
        salonServiceRepository.save(builtService);
    }

    private Set<Professional> getProfessionals(SalonServiceDTO dto) {
        if (dto.professionalsIds() == null || dto.professionalsIds().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(professionalRepository.findAllById(dto.professionalsIds()));
    }

    public List<SalonServiceOutDTO> getServices() {
        List<SalonService> services = new ArrayList<>(salonServiceRepository.findAllServices());
        return salonServiceMapper.mapToOutDTOList(services);
    }

    public List<SalonServiceOutDTO> getAllServicesForAdmin() {
        List<SalonService> services = salonServiceRepository.findAllWithInactive();
        return salonServiceMapper.mapToOutDTOList(services);
    }

    @Transactional
    public void changeSalonServiceVisibility(Long id, Boolean active) {

        salonServiceRepository.changeSalonServiceVisibility(id, active);
    }

    @Transactional
    public void updateSalonService(Long serviceId, SalonServiceDTO dto) {
        SalonService service = findById(serviceId);

        if (dto.name() != null) {
            service.setName(dto.name());
        }

        if (dto.description() != null) {
            service.setDescription(dto.description());
        }

        if (dto.durationInSeconds() != null) {
            service.setDurationInSeconds(dto.durationInSeconds());
        }

        if (dto.value() != null) {
            service.setValue(dto.value());
        }

        if (dto.professionalsIds() != null) {
            Set<Professional> professionals = new HashSet<>(professionalRepository.findAllById(dto.professionalsIds()));
            service.setProfessionals(professionals);
        }
    }

    @Transactional
    public void deleteService(Long id) {
        salonServiceRepository.deleteById(id);
    }
}