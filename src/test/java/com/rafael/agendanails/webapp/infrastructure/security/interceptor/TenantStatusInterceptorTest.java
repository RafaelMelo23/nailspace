package com.rafael.agendanails.webapp.infrastructure.security.interceptor;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.agendanails.webapp.infrastructure.security.util.SecurityPathManager;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantStatusInterceptorTest {

    @Mock
    private SalonProfileService salonProfileService;

    @Mock
    private SecurityPathManager securityPathManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TenantStatusInterceptor interceptor;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnTrueWhenTenantIsActive() throws Exception {
        String tenantId = "active-tenant";
        TenantContext.setTenant(tenantId);
        String path = "/api/appointments";

        when(request.getRequestURI()).thenReturn(path);

        when(securityPathManager.isInfrastructure(path)).thenReturn(false);
        when(securityPathManager.isPublicAccess(path)).thenReturn(false);
        when(salonProfileService.getStatusByTenantId(tenantId)).thenReturn(TenantStatus.ACTIVE);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void shouldReturnFalseAndSet402WhenTenantIsSuspended() throws Exception {
        String tenantId = "suspended-tenant";
        TenantContext.setTenant(tenantId);
        String path = "/api/appointments";

        when(request.getRequestURI()).thenReturn(path);
        when(securityPathManager.isInfrastructure(path)).thenReturn(false);
        when(securityPathManager.isPublicAccess(path)).thenReturn(false);
        when(salonProfileService.getStatusByTenantId(tenantId)).thenReturn(TenantStatus.SUSPENDED);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
        verify(response).setContentType("application/json");
        assertThat(stringWriter.toString()).contains("Acesso do locatário suspenso.");
    }

    @Test
    void shouldReturnTrueWhenPathIsInfrastructure() throws Exception {
        String path = "/css/style.css";
        when(request.getRequestURI()).thenReturn(path);
        when(securityPathManager.isInfrastructure(path)).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(salonProfileService, never()).getStatusByTenantId(anyString());
    }

    @Test
    void shouldReturnTrueWhenPathIsPublicAccess() throws Exception {
        String path = "/entrar";
        when(request.getRequestURI()).thenReturn(path);
        when(securityPathManager.isInfrastructure(path)).thenReturn(false);
        when(securityPathManager.isPublicAccess(path)).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(salonProfileService, never()).getStatusByTenantId(anyString());
    }

    @Test
    void shouldReturnTrueWhenNoTenantAndNotWhitelisted() throws Exception {
        String path = "/some-global-path";
        when(request.getRequestURI()).thenReturn(path);
        when(securityPathManager.isInfrastructure(path)).thenReturn(false);
        when(securityPathManager.isPublicAccess(path)).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(salonProfileService, never()).getStatusByTenantId(any());
    }
}