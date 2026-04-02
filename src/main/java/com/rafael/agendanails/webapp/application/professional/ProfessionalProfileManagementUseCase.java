package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.files.FileUploadService;
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

        String oldPic = professional.getProfessionalPicture();
        String newPic = fileUploadService.uploadBase64Image(pictureBase64);

        professional.setProfessionalPicture(newPic);

        if (oldPic != null && !oldPic.isEmpty()) {
            fileUploadService.delete(oldPic);
        }
    }
}