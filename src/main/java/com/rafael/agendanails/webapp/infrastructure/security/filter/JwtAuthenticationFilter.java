package com.rafael.agendanails.webapp.infrastructure.security.filter;

import com.rafael.agendanails.webapp.domain.enums.security.TokenClaim;
import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.shared.tenant.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.rafael.agendanails.webapp.domain.enums.security.TokenPurpose.AUTHENTICATION;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final TenantResolver tenantResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/webhook") || path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean isTenantContextSetByThisFilter = false;

        try {
            var token = jwtTokenService.recoverAndValidate(request);

            if (token != null) {
                String tokenPurposeClaim = token.getClaim("purpose").asString();

                if (AUTHENTICATION.getValue().equalsIgnoreCase(tokenPurposeClaim)) {

                    List<UserRole> userRoles = token.getClaim(TokenClaim.ROLE.getValue()).asList(String.class).stream()
                            .map(UserRole::fromString)
                            .toList();

                    Long userId = Long.parseLong(token.getSubject());
                    String userEmail = token.getClaim(TokenClaim.EMAIL.getValue()).asString();
                    String tokenTenantId = token.getClaim(TokenClaim.TENANT_ID.getValue()).asString();
                    Boolean isFirstLogin = token.getClaim(TokenClaim.FIRST_LOGIN.getValue()).asBoolean();

                    String currentTenant = tenantResolver.resolve(request);

                    if (currentTenant != null && tokenTenantId != null &&
                            !tokenTenantId.equalsIgnoreCase(currentTenant) &&
                            !userRoles.contains(UserRole.SUPER_ADMIN)) {
                        log.warn("Security mismatch: token for tenant [{}], but request for [{}]. User [{}] is not a SUPER_ADMIN. Authentication skipped.",
                                tokenTenantId, currentTenant, userEmail);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UserPrincipal userPrincipal = UserPrincipal.builder()
                            .userId(userId)
                            .email(userEmail)
                            .userRole(userRoles)
                            .tenantId(tokenTenantId)
                            .isFirstLogin(Boolean.TRUE.equals(isFirstLogin))
                            .build();

                    var authentication = new UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    if (TenantContext.getTenant() == null && tokenTenantId != null) {
                        TenantContext.setTenant(tokenTenantId);
                        MDC.put("tenant", tokenTenantId);
                        isTenantContextSetByThisFilter = true;
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            if (isTenantContextSetByThisFilter) {
                TenantContext.clear();
                MDC.remove("tenant");
            }
        }
    }
}
