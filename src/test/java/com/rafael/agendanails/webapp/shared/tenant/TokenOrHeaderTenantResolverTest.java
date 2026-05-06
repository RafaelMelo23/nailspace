package com.rafael.agendanails.webapp.shared.tenant;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenOrHeaderTenantResolverTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private TokenOrHeaderTenantResolver resolver;

    private DecodedJWT mockToken(String tenant) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim claim = mock(Claim.class);

        when(jwt.getClaim("tenantId")).thenReturn(claim);
        when(claim.asString()).thenReturn(tenant);

        return jwt;
    }

    @Test
    void shouldResolveTenantFromHeaderWhenTokenIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenantA");

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldResolveTenantFromTokenWhenHeaderMatches() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenantA");

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldResolveTenantFromTokenWhenHeaderIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Tenant-Id")).thenReturn(null);

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldResolveTenantFromTokenWhenNotHttpServletRequest() {
        ServletRequest request = mock(ServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(jwt);

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldNotThrowExceptionWhenTokenAndHeaderMatchWithDifferentCasing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Tenant-Id")).thenReturn("TENANTA");

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldPreferHeaderTenantWhenMismatchOccurs() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenantB");

        String tenant = resolver.resolve(request);

        assertEquals("tenantB", tenant);
    }

    @Test
    void shouldAllowMismatchForSuperAdmin() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim tenantClaim = mock(Claim.class);
        Claim rolesClaim = mock(Claim.class);

        when(jwt.getClaim("tenantId")).thenReturn(tenantClaim);
        when(tenantClaim.asString()).thenReturn("system");
        when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        when(rolesClaim.asList(String.class)).thenReturn(List.of("SUPER_ADMIN"));

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenantA");

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldReturnNullWhenTokenAndHeaderAreNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn(null);

        String tenant = resolver.resolve(request);

        assertNull(tenant);
    }
}