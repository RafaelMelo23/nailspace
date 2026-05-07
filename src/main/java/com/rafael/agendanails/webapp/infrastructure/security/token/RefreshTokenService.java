package com.rafael.agendanails.webapp.infrastructure.security.token;

import com.rafael.agendanails.webapp.domain.model.RefreshToken;
import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.domain.repository.RefreshTokenRepository;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.exception.TokenRefreshException;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
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
    private final UserRepository userRepository;
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
    public RefreshToken createRefreshToken(Long userId) {
        User user = getUserOrThrow(userId);
        return saveRefreshToken(user, defaultExpiry());
    }

    @IgnoreTenantFilter
    private RefreshToken saveRefreshToken(User user, Instant expiryDate) {
        return repository.save(
                RefreshToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(expiryDate)
                        .isRevoked(false)
                        .build()
        );
    }

    private Instant defaultExpiry() {
        return Instant.now(clock).plusMillis(tokenDurationMs);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new TokenRefreshException("Usuário não encontrado."));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllUserTokens(userId);
    }

    @Transactional
    public void revokeUserToken(String token, Long userId) {
        if (userId != null) {
            repository.revokeToken(token, userId);
            return;
        }

        repository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            repository.save(rt);
        });
    }

    @IgnoreTenantFilter
    @Transactional(noRollbackFor = TokenRefreshException.class)
    public RefreshToken rotateAndGetRefreshToken(String rawToken) {

        RefreshToken oldToken = repository.findByToken(rawToken)
                .orElseThrow(() -> new TokenRefreshException("Token não encontrado"));

        validateNotReused(oldToken);
        validateNotExpired(oldToken);

        User user = oldToken.getUser();
        oldToken.setRevoked(true);

        return saveRefreshToken(user, oldToken.getExpiryDate());
    }

    private void validateNotReused(RefreshToken token) {
        if (token.isRevoked()) {
            revokeAllForUser(token.getUser().getId());
            throw new TokenRefreshException("Este token já foi utilizado.");
        }
    }

    private void validateNotExpired(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now(clock))) {
            repository.delete(token);
            throw new TokenRefreshException("Expirado");
        }
    }
}