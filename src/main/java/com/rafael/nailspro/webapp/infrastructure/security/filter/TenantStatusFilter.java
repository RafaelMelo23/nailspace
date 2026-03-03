package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class TenantStatusFilter extends OncePerRequestFilter {

    private final SalonProfileService salonProfileService;

    //todo: create according front-end page to react to this filter

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String tenantId = TenantContext.getTenant();

        if (tenantId != null || !isAuthEndpoint(request)) {
            TenantStatus tenantStatus = salonProfileService.getStatusByTenantId(tenantId);

            if (tenantStatus == TenantStatus.SUSPENDED) {
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Subscription suspended\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    public boolean isAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();

        return !path.startsWith("/api/v1/auth") && !path.startsWith("/api/internal");
    }
}