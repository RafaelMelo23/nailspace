package com.rafael.nailspro.webapp.shared.tenant;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.nailspro.webapp.infrastructure.security.token.TokenService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.context.TenantIdentifierMismatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeaderOrSubdomainTenantResolverTest {

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private HeaderOrSubdomainTenantResolver resolver;

    private DecodedJWT mockToken(String tenant) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim claim = mock(Claim.class);

        when(jwt.getClaim("tenantId")).thenReturn(claim);
        when(claim.asString()).thenReturn(tenant);

        return jwt;
    }

    @Test
    void shouldResolveTenantFromSubdomainWhenTokenIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(tokenService.recoverAndValidate(request)).thenReturn(null);
        when(request.getHeader("X-Forwarded-Host")).thenReturn("tenantA.example.com");

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldResolveTenantFromHostHeaderWhenForwardedHostIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(tokenService.recoverAndValidate(request)).thenReturn(null);
        when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn("tenantB.example.com");

        String tenant = resolver.resolve(request);

        assertEquals("tenantB", tenant);
    }

    @Test
    void shouldResolveTenantFromServerNameWhenHeadersAreNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(tokenService.recoverAndValidate(request)).thenReturn(null);
        when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn(null);
        when(request.getServerName()).thenReturn("tenantC.example.com");

        String tenant = resolver.resolve(request);

        assertEquals("tenantC", tenant);
    }

    @Test
    void shouldResolveTenantFromTokenWhenSubdomainMatches() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        DecodedJWT jwt = mockToken("tenantA");

        when(tokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Forwarded-Host")).thenReturn("tenantA.example.com");

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldResolveTenantFromTokenWhenSubdomainIsTrulyNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(tokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn(null);
        when(request.getServerName()).thenReturn(null);

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldResolveTenantFromTokenWhenNotHttpServletRequest() {
        ServletRequest request = mock(ServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(tokenService.recoverAndValidate(request)).thenReturn(jwt);

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldNotThrowExceptionWhenTokenAndSubdomainMatchWithDifferentCasing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        DecodedJWT jwt = mockToken("tenantA");

        when(tokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Forwarded-Host")).thenReturn("TENANTA.example.com");

        String tenant = resolver.resolve(request);

        assertEquals("tenantA", tenant);
    }

    @Test
    void shouldThrowExceptionWhenTokenTenantAndSubdomainMismatch() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        DecodedJWT jwt = mockToken("tenantA");

        when(tokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Forwarded-Host")).thenReturn("tenantB.example.com");

        assertThrows(
                TenantIdentifierMismatchException.class,
                () -> resolver.resolve(request)
        );
    }

    @Test
    void shouldResolveTenantFromSubdomainWhenTokenTenantIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        DecodedJWT jwt = mockToken(null);

        when(tokenService.recoverAndValidate(request)).thenReturn(jwt);
        when(request.getHeader("X-Forwarded-Host")).thenReturn("tenantD.example.com");

        String tenant = resolver.resolve(request);

        assertEquals("tenantD", tenant);
    }

    @Test
    void shouldReturnNullWhenTokenAndSubdomainAreNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(tokenService.recoverAndValidate(request)).thenReturn(null);
        when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
        when(request.getHeader("Host")).thenReturn(null);
        when(request.getServerName()).thenReturn(null);

        String tenant = resolver.resolve(request);

        assertNull(tenant);
    }
}