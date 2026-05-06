package com.rafael.agendanails.webapp.infrastructure.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenExpiryJob {

    private final RefreshTokenService service;

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredTokens() {
        service.deleteExpiredTokens();
    }
}