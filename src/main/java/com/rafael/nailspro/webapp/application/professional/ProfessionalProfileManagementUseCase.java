package com.rafael.nailspro.webapp.application.professional;

import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.repository.ProfessionalRepository;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.infrastructure.files.FileUploadService;
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
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        if (professional.getProfessionalPicture() != null) {
            String oldPic = professional.getProfessionalPicture();
            String newPic = fileUploadService.uploadBase64Image(pictureBase64);

            professional.setProfessionalPicture(newPic);
            fileUploadService.delete(oldPic);
        }
    }
}