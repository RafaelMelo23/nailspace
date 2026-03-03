package com.rafael.nailspro.webapp.infrastructure.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.nailspro.webapp.domain.enums.security.TokenClaim;
import com.rafael.nailspro.webapp.domain.enums.security.TokenPurpose;
import com.rafael.nailspro.webapp.domain.model.User;
import com.rafael.nailspro.webapp.infrastructure.dto.auth.ResetPasswordDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    @Value("${api.security.jwt.secret}")
    private String secret;
    private static final String ISSUER_CLAIM = "nailspro-api";
    private static final String TENANT_CLAIM = "tenantId";
    private static final String PURPOSE_CLAIM = "purpose";

    public String generateAuthToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer(ISSUER_CLAIM)
                    .withSubject(user.getId().toString())
                    .withClaim(TokenClaim.EMAIL.getValue(), user.getEmail())
                    .withClaim(TokenClaim.ROLE.getValue(), user.getUserRole().getRole())
                    .withClaim(TokenClaim.TENANT_ID.getValue(), user.getTenantId())
                    .withClaim(TENANT_CLAIM, user.getTenantId())
                    .withClaim(PURPOSE_CLAIM, TokenPurpose.AUTHENTICATION.getValue())
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

            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                return authorizationHeader.substring(7);
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
                    .withClaim(PURPOSE_CLAIM, TokenPurpose.RESET_PASSWORD.getValue())
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
