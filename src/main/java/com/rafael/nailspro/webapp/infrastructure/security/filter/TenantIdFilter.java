package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import com.rafael.nailspro.webapp.shared.tenant.TenantResolver;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantIdFilter implements Filter {

    private final TenantResolver tenantResolver;

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain
    ) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            String path = request.getRequestURI();

            if (path.startsWith("/api/v1/webhook")
                    || path.startsWith("/api/internal")
                    || path.startsWith("/public")) {

                filterChain.doFilter(request, response);
                return;
            }

            String tenantId = tenantResolver.resolve(request);

            if (tenantId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant cannot be null");
                return;
            }

            MDC.put("tenant", tenantId);
            TenantContext.setTenant(tenantId);

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            MDC.remove("tenant");
        }
    }
}