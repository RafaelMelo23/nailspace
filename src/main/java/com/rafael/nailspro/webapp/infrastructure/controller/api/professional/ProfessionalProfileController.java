package com.rafael.nailspro.webapp.infrastructure.controller.api.professional;

import com.rafael.nailspro.webapp.application.professional.ProfessionalProfileManagementUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/professional/profile")
@RequiredArgsConstructor
public class ProfessionalProfileController {

    private final ProfessionalProfileManagementUseCase profileService;

    @PatchMapping("/{id}/picture")
    public ResponseEntity<Void> updatePicture(
            @PathVariable Long id,
            @RequestBody String pictureBase64
    ) throws IOException {
        profileService.updateProfilePicture(id, pictureBase64);
        return ResponseEntity.noContent().build();
    }
}