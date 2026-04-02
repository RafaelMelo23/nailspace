package com.rafael.agendanails.webapp.infrastructure.security.filter;

import com.rafael.agendanails.webapp.domain.enums.security.TokenClaim;
import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.security.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.rafael.agendanails.webapp.domain.enums.security.TokenPurpose.AUTHENTICATION;

@Slf4j
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

                List<UserRole> userRoles = token.getClaim(TokenClaim.ROLE.getValue()).asList(String.class).stream()
                        .map(UserRole::fromString)
                        .toList();

                Long userId = Long.parseLong(token.getSubject());
                String userEmail = token.getClaim(TokenClaim.EMAIL.getValue()).asString();
                String tenantId = token.getClaim(TokenClaim.TENANT_ID.getValue()).asString();

                UserPrincipal userPrincipal = UserPrincipal.builder()
                        .userId(userId)
                        .email(userEmail)
                        .userRole(userRoles)
                        .tenantId(tenantId)
                        .build();

                log.info("Authorities: {}", userPrincipal.getAuthorities());

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
