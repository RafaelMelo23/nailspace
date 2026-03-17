package com.rafael.nailspro.webapp.infrastructure.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.nailspro.webapp.domain.enums.security.TokenClaim;
import com.rafael.nailspro.webapp.domain.enums.security.TokenPurpose;
import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.infrastructure.dto.auth.ResetPasswordDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.support.user.TestUserFactory;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private TokenService tokenService;
    private static final String ISSUER_CLAIM = "nailspro-api";
    private static final String TENANT_CLAIM = "tenantId";
    private static final String PURPOSE_CLAIM = "purpose";

    @BeforeEach
    void setup() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret");
    }

    @Test
    void generateAuthToken_createsJwt() {
        Client user = TestUserFactory.client();

        String token = tokenService.generateAuthToken(user);

        assertNotNull(token);
    }

    @Test
    void generateAuthToken_createsCorrectClaims() {
        Client user = TestUserFactory.client();

        String token = tokenService.generateAuthToken(user);
        DecodedJWT decodedJWT = JWT.decode(token);

        assertEquals(ISSUER_CLAIM, decodedJWT.getIssuer());
        assertEquals(user.getId().toString(), decodedJWT.getSubject());

        assertEquals(user.getEmail(),
                decodedJWT.getClaim(TokenClaim.EMAIL.getValue()).asString());

        assertEquals(user.getUserRole().getRole(),
                decodedJWT.getClaim(TokenClaim.ROLE.getValue()).asString());

        assertEquals(user.getTenantId(),
                decodedJWT.getClaim(TokenClaim.TENANT_ID.getValue()).asString());

        assertEquals(user.getTenantId(),
                decodedJWT.getClaim(TENANT_CLAIM).asString());

        assertEquals(TokenPurpose.AUTHENTICATION.getValue(),
                decodedJWT.getClaim(PURPOSE_CLAIM).asString());

        assertNotNull(decodedJWT.getExpiresAt());
    }

    @Test
    void recover_successfullyExtractsToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Client user = TestUserFactory.client();
        String token = tokenService.generateAuthToken(user);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertEquals(token, tokenService.recover(request));
    }

    @Test
    void recover_returnsNullIfAuthNotBearer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Client user = TestUserFactory.client();
        String token = tokenService.generateAuthToken(user);

        when(request.getHeader("Authorization")).thenReturn(token);

        assertNull(tokenService.recover(request));
    }

    @Test
    void recover_returnsNullIfAuthMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        assertNull(tokenService.recover(request));
    }

    @Test
    void recover_returnsNullIfRequestNotInstanceOfHttpServlet() {
        ServletRequest request = mock(ServletRequest.class);

        assertNull(tokenService.recover(request));
    }

    @Test
    void validateAndDecode_returnsDecodedJwtWhenValid() {
        Client user = TestUserFactory.client();
        String token = tokenService.generateAuthToken(user);

        DecodedJWT decodedJWT = tokenService.validateAndDecode(token);

        assertNotNull(decodedJWT);
        assertEquals(user.getId().toString(), decodedJWT.getSubject());
    }

    @Test
    void validateAndDecode_returnsNullWhenInvalidToken() {
        assertNull(tokenService.validateAndDecode("invalid-token"));
    }

    @Test
    void validateAndDecode_returnsNullWhenNullToken() {
        assertNull(tokenService.validateAndDecode(null));
    }

    @Test
    void generateResetPasswordToken_createsCorrectClaims() {
        Long userId = 1L;

        String token = tokenService.generateResetPasswordToken(userId);
        DecodedJWT decodedJWT = JWT.decode(token);

        assertEquals(ISSUER_CLAIM, decodedJWT.getIssuer());
        assertEquals(userId.toString(), decodedJWT.getSubject());
        assertEquals(TokenPurpose.RESET_PASSWORD.getValue(), decodedJWT.getClaim(PURPOSE_CLAIM).asString());
        assertNotNull(decodedJWT.getExpiresAt());
    }

    @Test
    void validateResetPasswordToken_passesWhenValid() {
        Long userId = 1L;
        String token = tokenService.generateResetPasswordToken(userId);
        ResetPasswordDTO dto = mock(ResetPasswordDTO.class);

        when(dto.resetToken()).thenReturn(token);

        assertDoesNotThrow(() -> tokenService.validateResetPasswordToken(dto));
    }

    @Test
    void validateResetPasswordToken_throwsBusinessExceptionWhenWrongPurpose() {
        Client user = TestUserFactory.client();
        String token = tokenService.generateAuthToken(user);
        ResetPasswordDTO dto = mock(ResetPasswordDTO.class);

        when(dto.resetToken()).thenReturn(token);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tokenService.validateResetPasswordToken(dto));

        assertEquals("Informações inválidas. Tente novamente", exception.getMessage());
    }

    @Test
    void recoverAndValidate_returnsDecodedJwtWhenValid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Client user = TestUserFactory.client();
        String token = tokenService.generateAuthToken(user);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        DecodedJWT decodedJWT = tokenService.recoverAndValidate(request);

        assertNotNull(decodedJWT);
        assertEquals(user.getId().toString(), decodedJWT.getSubject());
    }
}