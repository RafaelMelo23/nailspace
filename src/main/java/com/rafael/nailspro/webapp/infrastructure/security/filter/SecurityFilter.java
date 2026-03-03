package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.rafael.nailspro.webapp.domain.enums.security.TokenClaim;
import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.model.UserPrincipal;
import com.rafael.nailspro.webapp.infrastructure.security.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.rafael.nailspro.webapp.domain.enums.security.TokenPurpose.AUTHENTICATION;

@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var token = tokenService.recoverAndValidate(request);

        if (token != null) {
            String tokenPurposeClaim = token.getClaim("purpose").asString();

            if (AUTHENTICATION.getValue().equalsIgnoreCase(tokenPurposeClaim)) {

                UserRole userRole = UserRole.fromString(token.getClaim(TokenClaim.ROLE.getValue()).asString());
                Long userId = Long.parseLong(token.getSubject());
                String userEmail = token.getClaim(TokenClaim.EMAIL.getValue()).asString();
                String tenantId = token.getClaim(TokenClaim.TENANT_ID.getValue()).asString();

                UserPrincipal userPrincipal = UserPrincipal.builder()
                        .userId(userId)
                        .email(userEmail)
                        .userRole(userRole)
                        .tenantId(tenantId)
                        .build();

                var authentication = new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
