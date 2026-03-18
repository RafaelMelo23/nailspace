package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import com.rafael.nailspro.webapp.shared.tenant.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Import(TenantIdFilter.class)
class TenantIdFilterWebMvcTest {

    @Mock
    private TenantResolver tenantResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private TenantIdFilter filter;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    void doFilter_ignores_BypassWebhookPath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/webhook");

        filter.doFilter(request, response, filterChain);
        verify(tenantResolver, never()).resolve(request);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ignores_BypassInternalPath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/internal");

        filter.doFilter(request, response, filterChain);
        verify(tenantResolver, never()).resolve(request);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ignores_BypassPublicPath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/public");

        filter.doFilter(request, response, filterChain);
        verify(tenantResolver, never()).resolve(request);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_returnsBadRequest_IfTenantNull() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/booking");

        when(tenantResolver.resolve(request)).thenReturn(null);
        filter.doFilter(request, response, filterChain);
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant cannot be null");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_successfully_setsTenantContext() throws ServletException, IOException {
        String tenantId = "tenantA";
        when(request.getRequestURI()).thenReturn("/api/v1/booking");
        when(tenantResolver.resolve(request)).thenReturn(tenantId);

        doAnswer(invocation -> {
            assertEquals(tenantId, TenantContext.getTenant());
            assertEquals(tenantId, MDC.get("tenant"));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(TenantContext.getTenant());
        assertNull(MDC.get("tenant"));
    }

    @Test
    void doFilter_removesTenantContext_evenOnException() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/booking");
        when(tenantResolver.resolve(request)).thenReturn("tenantA");

        filter.doFilter(request, response, filterChain);

        assertNull(TenantContext.getTenant());
        assertNull(MDC.get("tenantId"));
    }
}