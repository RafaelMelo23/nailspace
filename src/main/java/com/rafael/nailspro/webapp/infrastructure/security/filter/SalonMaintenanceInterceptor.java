package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SalonMaintenanceInterceptor implements HandlerInterceptor {

    private final SalonProfileService salonProfileService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getRequestURI();
        boolean isWhitelisted = isPathWhitelisted(path);

        if (TenantContext.getTenant() != null && !isWhitelisted) {
            if (salonProfileService.isSalonOpenByTenantId(TenantContext.getTenant())) {
                request.getRequestDispatcher("/offline").forward(request, response);
                return false;
            }
        }
        return true;
    }

    private static boolean isPathWhitelisted(String path) {
        return path.startsWith("/admin") ||
                path.startsWith("/api/v1/auth") ||
                path.contains("/css/") ||
                path.contains("/js/") ||
                path.contains("/entrar") ||
                path.startsWith("/public") ||
                path.startsWith("/api/internal") ||
                path.startsWith("/api/v1/webhook");
    }
}