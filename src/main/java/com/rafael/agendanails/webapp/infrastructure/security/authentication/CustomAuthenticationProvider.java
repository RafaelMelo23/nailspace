package com.rafael.agendanails.webapp.infrastructure.security.authentication;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.exception.LoginException;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.rafael.agendanails.webapp.domain.enums.user.UserStatus.BANNED;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final String DUMMY_PASSWORD = "$2a$12$p1DeDmHwMBRxNGAJ7II9JefEvHnrPDxCw72YF0nh1Modhwv67y1hK";

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String email = authentication.getName();
        String password = (String) authentication.getCredentials();

        UserPrincipal user = null;
        String storedPassword;

        try {
            user = (UserPrincipal) userDetailsService.loadUserByUsername(email);
            storedPassword = user.getPassword();
        } catch (Exception e) {
            storedPassword = DUMMY_PASSWORD;
        }

        if (!passwordEncoder.matches(password, storedPassword)) {
            throw new LoginException("Os dados informados são inválidos");
        }

        if (user == null) {
            throw new LoginException("Os dados informados são inválidos");
        }

        checkUserStatus(user);
        checkUsersTenant(user);

        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }

    protected static void checkUsersTenant(UserPrincipal user) {
        if (user.getUserRole().contains(UserRole.SUPER_ADMIN)) return;

        String currentTenant = TenantContext.getTenant();
        if (currentTenant != null && !user.getTenantId().equals(currentTenant)) {
            throw new LoginException("Acesso negado para este estabelecimento.");
        }
    }

    protected static void checkUserStatus(UserPrincipal user) {
        if (user.getUserStatus().equals(BANNED)) {
            throw new LoginException("Você foi banido deste estabelecimento");
        }
    }
}
