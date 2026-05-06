package com.rafael.agendanails.webapp.infrastructure.security.interceptor;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.infrastructure.security.util.SecurityPathManager;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SalonMaintenanceInterceptor implements HandlerInterceptor {

    private final SalonProfileService salonProfileService;
    private final SecurityPathManager securityPathManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String tenantId = TenantContext.getTenant();

        var isWhiteListed = securityPathManager.shouldIgnoreMaintenance(path);

        if (tenantId != null && !isWhiteListed) {
            if (!salonProfileService.isSalonOpenByTenantId(tenantId)) {
                if (path.startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    response.setHeader("X-Salon-State", "CLOSED");
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\": [\"O estabelecimento encontra-se fechado no momento.\"]}");
                    return false;
                }
                response.sendRedirect("/offline");
                return false;
            }
        }
        return true;
    }
}
