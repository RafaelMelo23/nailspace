package com.rafael.agendanails.webapp.infrastructure.security.token;

import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.RefreshToken;
import com.rafael.agendanails.webapp.domain.repository.RefreshTokenRepository;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.exception.TokenRefreshException;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshJwtTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private UserRepository userRepository;

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
        Client user = TestClientFactory.standard();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        RefreshToken savedToken = refreshTokenService.createRefreshToken(user.getId());
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
    void rotateAndGetRefreshToken_returnTokenIfNotExpired() {
        Client user = TestClientFactory.standard();
        user.setId(1L);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .expiryDate(Instant.now().plusMillis(tokenDurationMs))
                .isRevoked(false)
                .build();

        when(repository.findByToken("old-token")).thenReturn(Optional.of(token));
        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken newToken = refreshTokenService.rotateAndGetRefreshToken("old-token");
        
        assertNotNull(newToken);
        assertTrue(token.isRevoked());
        assertEquals(user, newToken.getUser());
    }

    @Test
    void rotateAndGetRefreshToken_throwsExceptionIfExpired() {
        RefreshToken token = RefreshToken.builder()
                .expiryDate(Instant.now().minus(tokenDurationMs, ChronoUnit.MILLIS))
                .build();

        when(repository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(
                TokenRefreshException.class,
                () -> refreshTokenService.rotateAndGetRefreshToken("expired-token"));
        
        verify(repository).delete(token);
    }

    @Test
    void rotateAndGetRefreshToken_throwsExceptionIfReused() {
        Client user = TestClientFactory.standard();
        user.setId(1L);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .isRevoked(true)
                .build();

        when(repository.findByToken("reused-token")).thenReturn(Optional.of(token));

        assertThrows(
                TokenRefreshException.class,
                () -> refreshTokenService.rotateAndGetRefreshToken("reused-token"));
        
        verify(repository).revokeAllUserTokens(user.getId());
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
}