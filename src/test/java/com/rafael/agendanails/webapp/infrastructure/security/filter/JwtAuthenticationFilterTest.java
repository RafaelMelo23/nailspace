package com.rafael.agendanails.webapp.infrastructure.security.filter;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.agendanails.webapp.domain.enums.security.TokenClaim;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.shared.tenant.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private TenantResolver tenantResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private DecodedJWT decodedJWT;
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    void shouldReturnTrueWhenPathIsWebhook() {
        when(request.getRequestURI()).thenReturn("/api/v1/webhook/events");

        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        assertThat(result).isTrue();
    }

    @Test
    void shouldSetAuthenticationWhenTokenIsValid() throws ServletException, IOException {
        String userId = "123";
        String email = "test@example.com";
        String role = "ADMIN";
        String tenantId = "tenant-001";

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(decodedJWT);
        when(tenantResolver.resolve(request)).thenReturn(tenantId);

        Claim purposeClaim = mock(Claim.class);
        when(purposeClaim.asString()).thenReturn("AUTHENTICATION");
        when(decodedJWT.getClaim("purpose")).thenReturn(purposeClaim);

        when(decodedJWT.getSubject()).thenReturn(userId);

        Claim roleClaim = mock(Claim.class);
        when(roleClaim.asList(String.class)).thenReturn(List.of(role));
        when(decodedJWT.getClaim(TokenClaim.ROLE.getValue())).thenReturn(roleClaim);

        setupClaimMock(TokenClaim.EMAIL.getValue(), email);
        setupClaimMock(TokenClaim.TENANT_ID.getValue(), tenantId);

        Claim firstLoginClaim = mock(Claim.class);
        when(firstLoginClaim.asBoolean()).thenReturn(false);
        when(decodedJWT.getClaim(TokenClaim.FIRST_LOGIN.getValue())).thenReturn(firstLoginClaim);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isNotNull();
        verify(filterChain).doFilter(request, response);

        assertThat(TenantContext.getTenant()).isNull();
        assertThat(MDC.get("tenant")).isNull();
    }

    @Test
    void shouldSetTenantContextIfMissing() throws ServletException, IOException {
        String userId = "123";
        String email = "test@example.com";
        String role = "ADMIN";
        String tenantId = "tenant-001";

        when(jwtTokenService.recoverAndValidate(request)).thenReturn(decodedJWT);
        when(tenantResolver.resolve(request)).thenReturn(tenantId);

        Claim purposeClaim = mock(Claim.class);
        when(purposeClaim.asString()).thenReturn("AUTHENTICATION");
        when(decodedJWT.getClaim("purpose")).thenReturn(purposeClaim);

        when(decodedJWT.getSubject()).thenReturn(userId);

        Claim roleClaim = mock(Claim.class);
        when(roleClaim.asList(String.class)).thenReturn(List.of(role));
        when(decodedJWT.getClaim(TokenClaim.ROLE.getValue())).thenReturn(roleClaim);

        setupClaimMock(TokenClaim.EMAIL.getValue(), email);
        setupClaimMock(TokenClaim.TENANT_ID.getValue(), tenantId);

        Claim firstLoginClaim = mock(Claim.class);
        when(firstLoginClaim.asBoolean()).thenReturn(false);
        when(decodedJWT.getClaim(TokenClaim.FIRST_LOGIN.getValue())).thenReturn(firstLoginClaim);

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenant()).isEqualTo(tenantId);
            assertThat(MDC.get("tenant")).isEqualTo(tenantId);
            return null;
        }).when(filterChain).doFilter(request, response);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.getTenant()).isNull();
        assertThat(MDC.get("tenant")).isNull();
    }

    @Test
    void shouldContinueChainWithoutAuthWhenTokenIsNull() throws ServletException, IOException {
        when(jwtTokenService.recoverAndValidate(request)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueChainWithoutAuthWhenPurposeIsInvalid() throws ServletException, IOException {
        when(jwtTokenService.recoverAndValidate(request)).thenReturn(decodedJWT);

        Claim purposeClaim = mock(Claim.class);
        when(purposeClaim.asString()).thenReturn("RECOVERY");
        when(decodedJWT.getClaim("purpose")).thenReturn(purposeClaim);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private void setupClaimMock(String claimName, String value) {
        Claim claim = mock(Claim.class);
        when(claim.asString()).thenReturn(value);
        when(decodedJWT.getClaim(claimName)).thenReturn(claim);
    }
}
