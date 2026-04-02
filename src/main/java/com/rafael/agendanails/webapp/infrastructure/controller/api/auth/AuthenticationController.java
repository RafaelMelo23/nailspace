package com.rafael.agendanails.webapp.infrastructure.controller.api.auth;

import com.rafael.agendanails.webapp.application.auth.AuthenticationService;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.config.SwaggerExamples;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.AuthResultDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.LoginDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.RegisterDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.TokenRefreshResponseDTO;
import com.rafael.agendanails.webapp.infrastructure.security.token.CookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication and session management")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final CookieService cookieService;

    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns a JWT. Also sets the refresh_token cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT token returned",
                    content = @Content(schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "jwt-token"))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginDTO.class),
                    examples = @ExampleObject(name = "LoginRequest", value = SwaggerExamples.LOGIN_REQUEST))
    )
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO,
                                        HttpServletResponse response) {

        AuthResultDTO authResultDTO = authenticationService.login(loginDTO);

        cookieService.addAccessTokenCookie(response, authResultDTO.jwtToken());
        cookieService.addRefreshTokenCookie(response, authResultDTO.refreshToken());

        return ResponseEntity.ok().body(authResultDTO.jwtToken());
    }

    @Operation(
            summary = "Logout",
            description = "Invalidates the refresh token and removes the refresh_token cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out"),
            @ApiResponse(responseCode = "400", description = "Refresh token not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       HttpServletRequest request, HttpServletResponse response) {

        if (Arrays.stream(request.getCookies()).findAny().isPresent()) {
            Optional<Cookie> refreshToken = Arrays.stream(request.getCookies())
                    .filter(c -> "refresh_token".equalsIgnoreCase(c.getName()))
                    .findFirst();

            if (refreshToken.isPresent()) {
                authenticationService.logout(refreshToken.get().getValue(), userPrincipal.getUserId());
                cookieService.deleteRefreshTokenCookie(response);
            }

            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(
            summary = "Register",
            description = "Registers a new client user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Validation error or user already exists")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegisterDTO.class),
                    examples = @ExampleObject(name = "RegisterRequest", value = SwaggerExamples.REGISTER_REQUEST))
    )
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {

        authenticationService.register(registerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "Refresh JWT",
            description = "Refreshes the JWT using the refresh_token cookie and returns a new JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT token returned",
                    content = @Content(schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "jwt-token"))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(
            @Parameter(in = ParameterIn.COOKIE, name = "refresh_token", required = true, example = "refresh-token-exemplo")
            @CookieValue(name = "refresh_token") String refreshToken,
                                          HttpServletResponse response) {

        TokenRefreshResponseDTO tokenRefreshResponseDTO = authenticationService.refreshToken(refreshToken);

        cookieService.addRefreshTokenCookie(response, tokenRefreshResponseDTO.refreshToken());

        return ResponseEntity.ok(tokenRefreshResponseDTO.jwtToken());
    }
}
