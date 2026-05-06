package com.rafael.agendanails.webapp.shared.tenant;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenOrHeaderTenantResolver implements TenantResolver {

    private final JwtTokenService jwtTokenService;

    private static final Set<String> RESERVED_PATHS = java.util.Set.of(
            "api", "js", "css", "assets", "pages", "favicon.svg", "favicon.ico",
            "error", "uploads", "public", "swagger-ui", "v3", "agendar", "entrar",
            "cadastro", "perfil", "offline", "redefinir-senha", "admin", "profissional"
    );

    @Override
    public String resolve(ServletRequest servletRequest) {
        DecodedJWT token = jwtTokenService.recoverAndValidate(servletRequest);

        String tenantFromToken = (token != null)
                ? token.getClaim("tenantId").asString()
                : null;

        String tenantFromRequest = null;

        if (servletRequest instanceof HttpServletRequest request) {
            tenantFromRequest = request.getHeader("X-Tenant-Id");

            if (tenantFromRequest == null) {
                String path = request.getRequestURI();
                if (path != null && path.length() > 1) {
                    String[] parts = path.split("/");
                    if (parts.length > 1) {
                        String candidate = parts[1];
                        if (!RESERVED_PATHS.contains(candidate.toLowerCase())) {
                            tenantFromRequest = candidate;
                        }
                    }
                }
            }
        }

        if (tenantFromToken != null &&
                tenantFromRequest != null &&
                !tenantFromToken.equalsIgnoreCase(tenantFromRequest)) {
            
            if (isSuperAdmin(token)) {
                log.debug("Super Admin accessing tenant [{}]. Token tenant is [{}].", tenantFromRequest, tenantFromToken);
                return tenantFromRequest;
            }

            log.warn("Tenant mismatch between token [{}] and request [{}]. Using request tenant.", tenantFromToken, tenantFromRequest);
            return tenantFromRequest;
        }

        String resolved = (tenantFromToken != null)
                ? tenantFromToken
                : tenantFromRequest;

        log.debug("Tenant resolved: [{}]", resolved);
        return resolved;
    }

    private boolean isSuperAdmin(DecodedJWT token) {
        if (token == null) return false;
        try {
            var roles = token.getClaim("roles").asList(String.class);
            return roles != null && roles.contains("SUPER_ADMIN");
        } catch (Exception e) {
            return false;
        }
    }
}