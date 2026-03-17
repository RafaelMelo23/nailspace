package com.rafael.nailspro.webapp.shared.tenant;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.nailspro.webapp.infrastructure.security.token.TokenService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.TenantIdentifierMismatchException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeaderOrSubdomainTenantResolver implements TenantResolver {

    private final TokenService tokenService;

    @Override
    public String resolve(ServletRequest servletRequest) {
        DecodedJWT token = tokenService.recoverAndValidate(servletRequest);

        String tenantFromToken = (token != null)
                ? token.getClaim("tenantId").asString()
                : null;

        String tenantFromSubdomain = null;

        if (servletRequest instanceof HttpServletRequest request) {
            String host = request.getHeader("X-Forwarded-Host");
            if (host == null) host = request.getHeader("Host");
            if (host == null) host = request.getServerName();

            if (host != null) {
                tenantFromSubdomain = host.split("\\.")[0];
            }
        }

        if (tenantFromToken != null &&
                tenantFromSubdomain != null &&
                !tenantFromToken.equalsIgnoreCase(tenantFromSubdomain)) {
            throw new TenantIdentifierMismatchException("Tenant mismatch between token and domain");
        }

        String resolved = (tenantFromToken != null)
                ? tenantFromToken
                : tenantFromSubdomain;

        log.info("Tenant resolved: [{}]", resolved);
        return resolved;
    }
}