package com.rafael.agendanails.webapp.infrastructure.controller.api.professional;

import com.rafael.agendanails.webapp.application.professional.ProfessionalProfileManagementUseCase;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.UpdateProfilePictureDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/professional/profile")
@Tag(name = "Professional - Profile", description = "Professional profile management")
public class ProfessionalProfileController {

    private final ProfessionalProfileManagementUseCase profileService;

    @Operation(summary = "Update profile picture", description = "Updates the professional profile picture.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profile picture updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = com.rafael.agendanails.webapp.infrastructure.dto.professional.UpdateProfilePictureDTO.class),
                    examples = @ExampleObject(name = "ProfilePictureRequest", value = "{\"pictureBase64\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA\"}"))
    )
    @PatchMapping("/picture")
    public ResponseEntity<Void> updatePicture(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfilePictureDTO dto
    ) throws IOException {
        profileService.updateProfilePicture(userPrincipal.getUserId(), dto.pictureBase64());
        return ResponseEntity.noContent().build();
    }
}