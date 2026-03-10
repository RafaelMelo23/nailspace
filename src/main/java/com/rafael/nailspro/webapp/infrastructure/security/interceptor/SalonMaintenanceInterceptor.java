package com.rafael.nailspro.webapp.infrastructure.security.interceptor;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.infrastructure.security.RequestPolicyManager;
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
    private final RequestPolicyManager requestPolicyManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        boolean isWhiteListed = requestPolicyManager.isWhiteListed(path);

        if (TenantContext.getTenant() != null && !isWhiteListed) {
            if (!salonProfileService.isSalonOpenByTenantId(TenantContext.getTenant())) {
                request.getRequestDispatcher("/offline").forward(request, response);
                return false;
            }
        }
        return true;
    }
}