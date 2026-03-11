package com.rafael.nailspro.webapp.infrastructure.controller.api.professional;

import com.rafael.nailspro.webapp.application.professional.ProfessionalQueryService;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.ProfessionalSimplifiedDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/professional")
public class ProfessionalQueryController {

    private final ProfessionalQueryService professionalQueryService;

    @GetMapping("/simplified")
    public ResponseEntity<List<ProfessionalSimplifiedDTO>> getProfessionals() {

        return ResponseEntity.ok(professionalQueryService.findAllSimplified());
    }
}
