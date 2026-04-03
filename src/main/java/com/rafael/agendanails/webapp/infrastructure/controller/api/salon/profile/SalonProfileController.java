package com.rafael.agendanails.webapp.infrastructure.controller.api.salon.profile;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.infrastructure.dto.salon.profile.SalonProfilePublicDTO;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/salon/profile")
@Tag(name = "Salon Profile", description = "Public salon profile information")
public class SalonProfileController {

    private final SalonProfileService salonProfileService;

    @Operation(summary = "Get public salon profile", description = "Returns the public salon profile information for the current tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "404", description = "Salon not found")
    })
    @GetMapping
    public ResponseEntity<SalonProfilePublicDTO> getPublicProfile() {
        SalonProfile salon = salonProfileService.getByTenantId(TenantContext.getTenant());

        return ResponseEntity.ok(SalonProfilePublicDTO.builder()
                .tradeName(salon.getTradeName())
                .slogan(salon.getSlogan())
                .primaryColor(salon.getPrimaryColor())
                .comercialPhone(salon.getComercialPhone())
                .fullAddress(salon.getFullAddress())
                .socialMediaLink(salon.getSocialMediaLink())
                .build());
    }
}
