package com.rafael.agendanails.webapp.infrastructure.security.util;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class SecurityPathManager {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> INFRASTRUCTURE_PATHS = List.of(
            "/css/**", "/js/**", "/assets/**", "/images/**", "/webjars/**", "/offline", "/favicon.ico", "/favicon.svg", "/pages/**", "/api/v1/salon/profile", "/index.html"
    );

    private static final List<String> PUBLIC_ACCESS_PATHS = List.of(
            "/entrar", "/login", "/public/**", "/api/v1/auth/**"
    );

    private static final List<String> TENANT_MANAGEMENT_PATHS = List.of(
            "/admin/**", "/api/v1/admin/**", "/api/internal/**", "/api/v1/webhook/**"
    );

    public boolean isInfrastructure(String path) {
        return matchAny(INFRASTRUCTURE_PATHS, path);
    }

    public boolean isPublicAccess(String path) {
        return matchAny(PUBLIC_ACCESS_PATHS, path);
    }

    public boolean isTenantManagement(String path) {
        return matchAny(TENANT_MANAGEMENT_PATHS, path);
    }

    public boolean shouldIgnoreMaintenance(String path) {
        return isInfrastructure(path) || isPublicAccess(path) || isTenantManagement(path);
    }

    private boolean matchAny(List<String> patterns, String path) {
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}