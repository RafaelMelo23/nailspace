package com.rafael.agendanails.webapp.infrastructure.controller.api.user;

import com.rafael.agendanails.webapp.application.user.PasswordResetUseCase;
import com.rafael.agendanails.webapp.application.user.UserProfileManagementUseCase;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.config.SwaggerExamples;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.ChangeEmailRequestDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.ChangePhoneRequestDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.user.profile.ChangePasswordRequestDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.user.profile.UserProfileDto;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User", description = "Authenticated user profile and password management")
public class UserController {

    private final UserProfileManagementUseCase userService;
    private final PasswordResetUseCase passwordResetUseCase;

    @Operation(summary = "Get profile", description = "Returns the authenticated user's profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned",
                    content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getProfile(userPrincipal.getId()));
    }

    @Operation(summary = "Update email", description = "Updates the authenticated user's email.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or password mismatch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChangeEmailRequestDTO.class),
                    examples = @ExampleObject(name = "ChangeEmailRequest", value = SwaggerExamples.CHANGE_EMAIL_REQUEST))
    )
    @PatchMapping("/email")
    public ResponseEntity<Void> updateEmail(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                            @Valid @RequestBody ChangeEmailRequestDTO dto) {

        userService.updateEmail(userPrincipal.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update phone", description = "Updates the authenticated user's phone number.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Phone updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or password mismatch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChangePhoneRequestDTO.class),
                    examples = @ExampleObject(name = "ChangePhoneRequest", value = SwaggerExamples.CHANGE_PHONE_REQUEST))
    )
    @PatchMapping("/phone")
    public ResponseEntity<Void> updatePhone(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                            @Valid @RequestBody ChangePhoneRequestDTO dto) {

        userService.updatePhone(userPrincipal.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Change password", description = "Changes the authenticated user's password")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                               @RequestBody ChangePasswordRequestDTO dto) {
        userService.changePassword(userPrincipal.getId(), dto.email(), dto.newPassword());
        return ResponseEntity.noContent().build();
    }
}