package com.rafael.agendanails.webapp.infrastructure.security;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class RequestPolicyManager {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final List<String> WHITELIST = List.of(
            "/admin/**",
            "/api/v1/auth/**",
            "/css/**",
            "/js/**",
            "/entrar",
            "/public/**",
            "/api/internal/**",
            "/api/v1/webhook/**",
            "/api/v1/admin/**",
            "/api/v1/professional/simplified",
            "/api/v1/booking/*/availability",
            "/api/v1/salon/profile",
            "/api/v1/salon/service",
            "/offline"
    );

    public boolean isWhiteListed(String path) {
        return WHITELIST.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

}
