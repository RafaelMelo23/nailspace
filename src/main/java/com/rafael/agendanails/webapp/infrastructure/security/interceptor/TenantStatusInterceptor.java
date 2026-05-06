package com.rafael.agendanails.webapp.infrastructure.security.interceptor;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.agendanails.webapp.infrastructure.security.util.SecurityPathManager;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantStatusInterceptor implements HandlerInterceptor {

    private final SalonProfileService salonProfileService;
    private final SecurityPathManager securityPathManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getTenant();
        String path = request.getRequestURI();

        if (isWhiteListed(path)) {
            return true;
        }
        if (tenantId != null) {
            TenantStatus tenantStatus = salonProfileService.getStatusByTenantId(tenantId);

            if (tenantStatus == TenantStatus.SUSPENDED) {
                if (path.startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\": [\"Acesso do locatário suspenso.\"]}");
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    private boolean isWhiteListed(String path) {
        return securityPathManager.isInfrastructure(path) ||
                securityPathManager.isPublicAccess(path);
    }
}