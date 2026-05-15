package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.exception.DemoFeatureException;
import com.rafael.agendanails.webapp.infrastructure.files.FileUploadService;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ProfessionalProfileManagementUseCase {

    private final ProfessionalRepository professionalRepository;
    private final FileUploadService fileUploadService;

    @Transactional
    public void updateProfilePicture(Long professionalId, String pictureBase64) throws IOException {
        if ("demo-salon-2026".equals(TenantContext.getTenant())) {
            throw new DemoFeatureException("Esta funcionalidade está desativada para a versão de demonstração");
        }

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        String oldPic = professional.getProfessionalPicture();
        String newPic = fileUploadService.uploadBase64Image(pictureBase64);

        professional.updatePicture(newPic);

        if (oldPic != null && !oldPic.isEmpty()) {
            fileUploadService.delete(oldPic);
        }
    }
}