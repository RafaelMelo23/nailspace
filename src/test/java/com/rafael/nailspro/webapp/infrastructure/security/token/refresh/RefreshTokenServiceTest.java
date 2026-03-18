package com.rafael.nailspro.webapp.infrastructure.security.token.refresh;

import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.domain.model.RefreshToken;
import com.rafael.nailspro.webapp.domain.repository.RefreshTokenRepository;
import com.rafael.nailspro.webapp.infrastructure.exception.TokenRefreshException;
import com.rafael.nailspro.webapp.support.factory.TestUserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private final long tokenDurationMs = 60000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "tokenDurationMs", 60000L);
        ReflectionTestUtils.setField(refreshTokenService, "clock", Clock.systemUTC());
    }

    @Test
    void createRefreshToken_successfullyCreatesToken() {
        Client user = TestUserFactory.client();

        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        RefreshToken savedToken = refreshTokenService.createRefreshToken(user);
        Instant after = Instant.now();

        Instant expiry = savedToken.getExpiryDate();

        assertTrue(
                !expiry.isBefore(before.plusMillis(tokenDurationMs)) &&
                        !expiry.isAfter(after.plusMillis(tokenDurationMs))
        );

        assertNotNull(savedToken);
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getToken());
        assertNotNull(savedToken.getExpiryDate());
        assertFalse(savedToken.isRevoked());
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void verifyExpiration_returnTokenIfNotExpired() {
        RefreshToken token = RefreshToken.builder()
                .expiryDate(Instant.now().plusMillis(tokenDurationMs))
                .build();

        RefreshToken validToken = refreshTokenService.verifyExpiration(token);
        assertNotNull(validToken);
        assertEquals(token, validToken);
    }

    @Test
    void verifyExpiration_throwsExceptionIfExpired() {
        RefreshToken token = RefreshToken.builder()
                .expiryDate(Instant.now().minus(tokenDurationMs, ChronoUnit.MILLIS))
                .build();

        assertThrows(
                TokenRefreshException.class,
                () -> refreshTokenService.verifyExpiration(token));
    }

    @Test void verifyExpiration_tokenExpiringNow_isStillValid() {
        ReflectionTestUtils.setField(refreshTokenService, "clock", Clock.fixed(Instant.parse("2026-03-18T18:00:00Z"), ZoneOffset.UTC));
        RefreshToken token = RefreshToken.builder().expiryDate(Instant.now()).build();
        assertDoesNotThrow(() -> refreshTokenService.verifyExpiration(token));
    }

    @Test
    void findByToken_delegatesToRepository() {
        String tokenStr = "test-token-uuid";
        RefreshToken token = RefreshToken.builder().token(tokenStr).build();
        when(repository.findByToken(tokenStr)).thenReturn(java.util.Optional.of(token));

        java.util.Optional<RefreshToken> result = refreshTokenService.findByToken(tokenStr);

        assertTrue(result.isPresent());
        assertEquals(token, result.get());
        verify(repository).findByToken(tokenStr);
    }

    @Test
    void deleteExpiredTokens_delegatesToRepository() {
        refreshTokenService.deleteExpiredTokens();

        verify(repository).deleteByExpiryDateBefore(any(Instant.class));
    }

    @Test
    void revokeAllForUser_delegatesToRepository() {
        Long userId = 1L;

        refreshTokenService.revokeAllForUser(userId);

        verify(repository).revokeAllUserTokens(userId);
    }

    @Test
    void revokeUserToken_delegatesToRepository() {
        String tokenStr = "test-token-uuid";
        Long userId = 1L;

        refreshTokenService.revokeUserToken(tokenStr, userId);

        verify(repository).revokeToken(tokenStr, userId);
    }

    @Test
    void verifyExpiration_deletesTokenWhenExpired() {
        RefreshToken token = RefreshToken.builder()
                .expiryDate(Instant.now().minus(tokenDurationMs, ChronoUnit.MILLIS))
                .build();

        assertThrows(
                TokenRefreshException.class,
                () -> refreshTokenService.verifyExpiration(token)
        );

        verify(repository).delete(token);
    }
}