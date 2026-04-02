package com.rafael.agendanails.webapp.infrastructure.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.agendanails.webapp.domain.enums.security.TokenClaim;
import com.rafael.agendanails.webapp.domain.enums.security.TokenPurpose;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.ResetPasswordDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Service
public class TokenService {

    @Value("${api.security.jwt.secret}")
    private String secret;
    private static final String ISSUER_CLAIM = "agendanails-api";

    public String generateAuthToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            boolean isFirstLogin = false;
            if (user instanceof Professional prof) {
                isFirstLogin = prof.getIsFirstLogin();
            }

            return JWT.create()
                    .withIssuer(ISSUER_CLAIM)
                    .withSubject(user.getId().toString())
                    .withClaim(TokenClaim.EMAIL.getValue(), user.getEmail())
                    .withClaim(TokenClaim.ROLE.getValue(), user.getEffectiveRoles()
                            .stream()
                            .map(Enum::name)
                            .toList())
                    .withClaim(TokenClaim.TENANT_ID.getValue(), user.getTenantId())
                    .withClaim(TokenClaim.PURPOSE.getValue(), TokenPurpose.AUTHENTICATION.getValue())
                    .withClaim(TokenClaim.FIRST_LOGIN.getValue(), isFirstLogin)
                    .withExpiresAt(generateAuthExpirationTime())
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new JWTCreationException("Failed to create JWT", e);
        }
    }

    public DecodedJWT recoverAndValidate(ServletRequest servletRequest) {

        return validateAndDecode(recover(servletRequest));
    }

    public String recover(ServletRequest servletRequest) {
        if (servletRequest instanceof HttpServletRequest request) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }

            if (request.getCookies() != null) {
                return Arrays.stream(request.getCookies())
                        .filter(c -> "access_token".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    public DecodedJWT validateAndDecode(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER_CLAIM)
                    .build();

            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public String generateResetPasswordToken(Long userId) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer(ISSUER_CLAIM)
                    .withSubject(userId.toString())
                    .withClaim(TokenClaim.PURPOSE.getValue(), TokenPurpose.RESET_PASSWORD.getValue())
                    .withExpiresAt(generateResetPasswordExpirationTime())
                    .sign(algorithm);

        } catch (JWTCreationException e) {
            throw new JWTCreationException("Failed to create reset password token", e);
        }
    }

    public void validateResetPasswordToken(ResetPasswordDTO resetPasswordDTO) {
        DecodedJWT token = validateAndDecode(resetPasswordDTO.resetToken());
        if (!TokenPurpose.RESET_PASSWORD.getValue().equalsIgnoreCase(token.getClaim("purpose").asString())) {
            throw new BusinessException("Informações inválidas. Tente novamente");
        }
    }

    public Instant generateAuthExpirationTime() {

        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    public Instant generateResetPasswordExpirationTime() {

        return Instant.now().plus(15, ChronoUnit.MINUTES);
    }
}
