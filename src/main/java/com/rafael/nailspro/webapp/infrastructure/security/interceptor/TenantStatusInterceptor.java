package com.rafael.nailspro.webapp.infrastructure.security.interceptor;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.infrastructure.security.RequestPolicyManager;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantStatusInterceptor implements HandlerInterceptor {

    private final SalonProfileService salonProfileService;
    private final RequestPolicyManager requestPolicyManager;

    //todo: create according front-end page to react to this filter

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getTenant();
        String path = request.getRequestURI();
        boolean isWhiteListed = requestPolicyManager.isWhiteListed(path);

        if (tenantId != null || !isWhiteListed) {
            TenantStatus tenantStatus = salonProfileService.getStatusByTenantId(tenantId);

            if (tenantStatus == TenantStatus.SUSPENDED) {
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Subscription suspended\"}");
                return false;
            }
        }
        return true;
    }
}