package com.rafael.nailspro.webapp.infrastructure.security.token.refresh;

import com.rafael.nailspro.webapp.domain.model.RefreshToken;
import com.rafael.nailspro.webapp.domain.model.User;
import com.rafael.nailspro.webapp.domain.repository.RefreshTokenRepository;
import com.rafael.nailspro.webapp.infrastructure.exception.TokenRefreshException;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final Clock clock;

    @Value("${app.jwt.refreshExpirationMs}")
    private Long tokenDurationMs;

    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    @Transactional
    @IgnoreTenantFilter
    public void deleteExpiredTokens() {
        repository.deleteByExpiryDateBefore(Instant.now(clock));
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        return saveRefreshToken(user);
    }

    private RefreshToken saveRefreshToken(User user) {
        return repository.save(
                RefreshToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(Instant.now(clock).plusMillis(tokenDurationMs))
                        .isRevoked(false)
                        .build()
        );
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now(clock))) {
            repository.delete(token);
            throw new TokenRefreshException("Expirado");
        }
        return token;
    }

    @Transactional
    public void revokeAllForUser(Long userId) {

        repository.revokeAllUserTokens(userId);
    }

    @Transactional
    public void revokeUserToken(String token, Long userId) {

        repository.revokeToken(token, userId);
    }
}